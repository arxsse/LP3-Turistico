<?php
$apiBaseUrl = getenv('TURISTICO_API_BASE') ?: 'http://turistas.spring.informaticapp.com:2410/api/v1/auth';

$registerResult = null;
$registerError = null;
$loginResult = null;
$loginError = null;

function callRemoteApi($url, array $payload)
{
	$curl = curl_init();
	curl_setopt_array($curl, array(
		CURLOPT_URL => $url,
		CURLOPT_RETURNTRANSFER => true,
		CURLOPT_ENCODING => '',
		CURLOPT_MAXREDIRS => 10,
		CURLOPT_TIMEOUT => 20,
		CURLOPT_FOLLOWLOCATION => true,
		CURLOPT_HTTP_VERSION => CURL_HTTP_VERSION_1_1,
		CURLOPT_CUSTOMREQUEST => 'POST',
		CURLOPT_POSTFIELDS => json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
		CURLOPT_HTTPHEADER => array('Content-Type: application/json'),
	));

	$response = curl_exec($curl);

	if ($response === false) {
		$error = curl_error($curl);
		curl_close($curl);
		return array(
			'status' => 0,
			'data' => null,
			'raw' => null,
			'error' => $error,
		);
	}

	$status = curl_getinfo($curl, CURLINFO_HTTP_CODE);
	curl_close($curl);

	return array(
		'status' => $status,
		'data' => json_decode($response),
		'raw' => $response,
		'error' => null,
	);
}

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
	$action = isset($_POST['action']) ? $_POST['action'] : '';

	if ($action === 'register') {
		$payload = array(
			'nombre' => trim($_POST['nombre'] ?? ''),
			'apellido' => trim($_POST['apellido'] ?? ''),
			'email' => trim($_POST['email'] ?? ''),
			'password' => $_POST['password'] ?? '',
		);

		$dni = trim($_POST['dni'] ?? '');
		if ($dni !== '') {
			$payload['dni'] = $dni;
		}

		$result = callRemoteApi($apiBaseUrl . '/register', $payload);

		if ($result['error']) {
			$registerError = 'No se pudo conectar con la API: ' . $result['error'];
		} elseif ($result['status'] < 200 || $result['status'] >= 300) {
			$registerError = 'Error al registrar usuario. HTTP ' . $result['status'] . '. Respuesta: ' . $result['raw'];
		} else {
			$registerResult = $result;
		}
	} elseif ($action === 'login') {
		$payload = array(
			'email' => trim($_POST['login_email'] ?? ''),
			'password' => $_POST['login_password'] ?? '',
		);

		$result = callRemoteApi($apiBaseUrl . '/login', $payload);

		if ($result['error']) {
			$loginError = 'No se pudo conectar con la API: ' . $result['error'];
		} elseif ($result['status'] < 200 || $result['status'] >= 300) {
			$loginError = 'Error al iniciar sesión. HTTP ' . $result['status'] . '. Respuesta: ' . $result['raw'];
		} else {
			$loginResult = $result;
		}
	}
}
?>

<!DOCTYPE html>
<html lang="es">
<head>
	<meta charset="UTF-8">
	<title>Sistema Turístico - Token Nube</title>
	<style>
		*,
		*::before,
		*::after {
			box-sizing: border-box;
		}

		:root {
			--bg-primary: #020617;
			--bg-secondary: #0f172a;
			--surface: rgba(15, 23, 42, 0.78);
			--panel: rgba(15, 23, 42, 0.55);
			--accent-green: #10b981;
			--accent-blue: #2563eb;
			--accent-red: #ef4444;
		}

		body {
			margin: 0;
			min-height: 100vh;
			display: flex;
			align-items: center;
			justify-content: center;
			font-family: "Segoe UI", Tahoma, Geneva, Verdana, sans-serif;
			color: #e2e8f0;
			background: radial-gradient(circle at 0% 0%, rgba(45, 212, 191, 0.18), transparent 65%), radial-gradient(circle at 100% 20%, rgba(96, 165, 250, 0.25), transparent 60%), linear-gradient(135deg, var(--bg-primary), var(--bg-secondary) 55%, #111827);
			background-size: 180% 180%;
			animation: gradientShift 22s ease infinite;
		}

		.page-shell {
			width: 100%;
			max-width: 1120px;
			padding: 4rem 1.5rem;
			display: flex;
			flex-direction: column;
			align-items: center;
			gap: 2rem;
		}

		.brand-badge {
			text-transform: uppercase;
			font-size: 0.85rem;
			letter-spacing: 0.3rem;
			color: rgba(226, 232, 240, 0.85);
			padding: 0.65rem 1.8rem;
			border: 1px solid rgba(148, 163, 184, 0.2);
			border-radius: 999px;
			background: rgba(15, 23, 42, 0.65);
			backdrop-filter: blur(12px);
			box-shadow: 0 20px 45px rgba(2, 6, 23, 0.45);
		}

		.surface {
			width: 100%;
			position: relative;
			z-index: 0;
			background: var(--surface);
			border-radius: 28px;
			padding: 3.5rem;
			border: 1px solid rgba(148, 163, 184, 0.12);
			box-shadow: 0 45px 90px rgba(2, 6, 23, 0.55);
			backdrop-filter: blur(18px);
			overflow: hidden;
		}

		.surface::before,
		.surface::after {
			content: "";
			position: absolute;
			z-index: -1;
			width: 65%;
			height: 65%;
			filter: blur(0);
			opacity: 0.55;
			animation: rotateGlow 20s linear infinite;
		}

		.surface::before {
			top: -40%;
			right: -25%;
			background: radial-gradient(circle, rgba(96, 165, 250, 0.4), transparent 70%);
		}

		.surface::after {
			bottom: -45%;
			left: -20%;
			animation-direction: reverse;
			background: radial-gradient(circle, rgba(34, 197, 94, 0.35), transparent 70%);
		}

		.surface-header {
			margin-bottom: 2.25rem;
		}

		.surface-header h1 {
			margin: 0;
			font-size: 2.4rem;
			font-weight: 700;
			color: #f8fafc;
		}

		.surface-header p {
			margin: 0.75rem 0 0;
			max-width: 520px;
			color: rgba(148, 163, 184, 0.9);
			line-height: 1.5;
		}

		.status-card {
			border-radius: 22px;
			padding: 1.75rem;
			margin-bottom: 1.75rem;
			border: 1px solid rgba(148, 163, 184, 0.18);
			box-shadow: 0 30px 70px rgba(2, 6, 23, 0.5);
		}

		.status-card h2 {
			margin: 0 0 0.75rem;
			font-size: 1.25rem;
			font-weight: 700;
		}

		.status-card p {
			margin: 0.35rem 0;
			color: rgba(226, 232, 240, 0.92);
			font-size: 0.95rem;
		}

		.status-card strong {
			color: #f8fafc;
		}

		.status-success {
			background: linear-gradient(135deg, rgba(16, 185, 129, 0.3), rgba(52, 211, 153, 0.16));
			border-color: rgba(74, 222, 128, 0.3);
		}

		.status-success h2 {
			color: #d1fae5;
		}

		.status-info {
			background: linear-gradient(135deg, rgba(59, 130, 246, 0.28), rgba(37, 99, 235, 0.16));
			border-color: rgba(96, 165, 250, 0.28);
		}

		.status-info h2 {
			color: #dbeafe;
		}

		.status-error {
			background: linear-gradient(135deg, rgba(239, 68, 68, 0.28), rgba(244, 114, 182, 0.18));
			border-color: rgba(248, 113, 113, 0.32);
		}

		.status-error h2 {
			color: #fee2e2;
		}

		.status-header {
			display: flex;
			align-items: center;
			justify-content: space-between;
			gap: 1rem;
			margin-bottom: 1.05rem;
		}

		.status-header h2 {
			margin: 0;
		}

		.status-token {
			margin-top: 0.35rem;
		}

		.status-token pre {
			margin: 0;
			padding: 1.2rem 1.5rem;
			border-radius: 16px;
			background: rgba(2, 6, 23, 0.85);
			color: #f8fafc;
			white-space: pre-wrap;
			word-break: break-word;
			font-size: 0.9rem;
			line-height: 1.5;
		}

		.copy-btn {
			border: 1px solid rgba(148, 163, 184, 0.25);
			border-radius: 999px;
			padding: 0.45rem 1.15rem;
			font-size: 0.78rem;
			font-weight: 600;
			letter-spacing: 0.03rem;
			background: rgba(148, 163, 184, 0.2);
			color: #e2e8f0;
			cursor: pointer;
			transition: background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;
		}

		.copy-btn:hover {
			background: rgba(148, 163, 184, 0.35);
			transform: translateY(-1px);
			box-shadow: 0 10px 30px rgba(2, 6, 23, 0.5);
		}

		.form-layout {
			display: grid;
			grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
			gap: 2rem;
		}

		.form-card {
			background: var(--panel);
			border-radius: 24px;
			padding: 2.4rem;
			border: 1px solid rgba(148, 163, 184, 0.12);
			box-shadow: 0 35px 65px rgba(2, 6, 23, 0.5);
			display: flex;
			flex-direction: column;
			gap: 1.75rem;
		}

		.form-card h2 {
			margin: 0;
			font-size: 1.5rem;
			font-weight: 600;
			color: #f8fafc;
		}

		.form-subtitle {
			margin: 0;
			color: rgba(148, 163, 184, 0.85);
			font-size: 0.95rem;
		}

		.form-stack {
			display: flex;
			flex-direction: column;
			gap: 1.25rem;
		}

		.field {
			display: flex;
			flex-direction: column;
			gap: 0.5rem;
		}

		.field label {
			font-weight: 600;
			font-size: 0.95rem;
			color: rgba(226, 232, 240, 0.95);
		}

		.control {
			padding: 0.85rem 1rem;
			border-radius: 14px;
			border: 1px solid rgba(148, 163, 184, 0.25);
			background: rgba(15, 23, 42, 0.78);
			color: #f8fafc;
			font-size: 0.95rem;
			transition: border 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
		}

		.control::placeholder {
			color: rgba(148, 163, 184, 0.65);
		}

		.control:focus {
			outline: none;
			border-color: rgba(59, 130, 246, 0.7);
			box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.18);
			background: rgba(2, 6, 23, 0.9);
		}

		.action-btn {
			border: none;
			border-radius: 14px;
			padding: 0.95rem 1.6rem;
			font-size: 1rem;
			font-weight: 600;
			letter-spacing: 0.02rem;
			color: #0f172a;
			cursor: pointer;
			transition: transform 0.2s ease, box-shadow 0.2s ease;
			align-self: flex-start;
		}

		.action-btn:hover {
			transform: translateY(-2px);
			box-shadow: 0 25px 40px rgba(2, 6, 23, 0.55);
		}

		.accent-green {
			background: linear-gradient(135deg, #34d399, #10b981);
			color: #022c22;
		}

		.accent-blue {
			background: linear-gradient(135deg, #60a5fa, #2563eb);
			color: #0b1120;
		}

		@media (max-width: 768px) {
			.surface {
				padding: 2.75rem 1.85rem;
			}

			.surface-header h1 {
				font-size: 2rem;
			}
		}

		@media (max-width: 520px) {
			.page-shell {
				padding: 3.25rem 1.2rem;
			}

			.brand-badge {
				letter-spacing: 0.22rem;
			}
		}

		@keyframes gradientShift {
			0%,
			100% {
				background-position: 0% 50%;
			}
			50% {
				background-position: 100% 50%;
			}
		}

		@keyframes rotateGlow {
			0% {
				transform: rotate(0deg);
			}
			100% {
				transform: rotate(360deg);
			}
		}
	</style>
</head>
<body>
<div class="page-shell">
	<div class="brand-badge">Sistema Turístico</div>
	<div class="surface">
		<div class="surface-header">
			<h1>Token Nube</h1>
			<p>Utiliza el entorno remoto para registrar usuarios o recuperar su token vigente.</p>
			<small style="color: rgba(148, 163, 184, 0.65); display: block; margin-top: 0.6rem;">Destino actual: <?php echo htmlspecialchars($apiBaseUrl, ENT_QUOTES, 'UTF-8'); ?></small>
		</div>

		<?php if ($registerResult && isset($registerResult['data']->token)) : ?>
			<div class="status-card status-success">
				<div class="status-header">
					<h2>Registro exitoso</h2>
					<button type="button" class="copy-btn" data-target="register-token">Copiar token</button>
				</div>
				<div class="status-token">
					<pre id="register-token"><code><?php echo htmlspecialchars($registerResult['data']->token, ENT_QUOTES, 'UTF-8'); ?></code></pre>
				</div>
			</div>
		<?php elseif ($registerError) : ?>
			<div class="status-card status-error">
				<h2>Ocurrió un problema</h2>
				<p><?php echo htmlspecialchars($registerError, ENT_QUOTES, 'UTF-8'); ?></p>
			</div>
		<?php endif; ?>

		<?php if ($loginResult && isset($loginResult['data']->token)) : ?>
			<div class="status-card status-info">
				<div class="status-header">
					<h2>Obtención del token exitosa</h2>
					<button type="button" class="copy-btn" data-target="login-token">Copiar token</button>
				</div>
				<div class="status-token">
					<pre id="login-token"><code><?php echo htmlspecialchars($loginResult['data']->token, ENT_QUOTES, 'UTF-8'); ?></code></pre>
				</div>
			</div>
		<?php elseif ($loginError) : ?>
			<div class="status-card status-error">
				<h2>No se pudo iniciar sesión</h2>
				<p><?php echo htmlspecialchars($loginError, ENT_QUOTES, 'UTF-8'); ?></p>
			</div>
		<?php endif; ?>

		<div class="form-layout">
			<div class="form-card">
				<div>
					<h2>Registrar</h2>
					<p class="form-subtitle">Crea un acceso remoto y recupera su credencial persistida.</p>
				</div>
				<form method="post" class="form-stack">
					<input type="hidden" name="action" value="register">
					<div class="field">
						<label for="nombre">Nombre</label>
						<input type="text" class="control" id="nombre" name="nombre" placeholder="Juan" required>
					</div>
					<div class="field">
						<label for="apellido">Apellido</label>
						<input type="text" class="control" id="apellido" name="apellido" placeholder="Pérez" required>
					</div>
					<div class="field">
						<label for="email">Email</label>
						<input type="email" class="control" id="email" name="email" placeholder="juan@example.com" required>
					</div>
					<div class="field">
						<label for="password">Contraseña</label>
						<input type="password" class="control" id="password" name="password" placeholder="Mínimo 6 caracteres" minlength="6" required>
					</div>
					<div class="field">
						<label for="dni">DNI (opcional)</label>
						<input type="text" class="control" id="dni" name="dni" placeholder="12345678">
					</div>
					<button type="submit" class="action-btn accent-green">Registrar</button>
				</form>
			</div>
			<div class="form-card">
				<div>
					<h2>Obtener token</h2>
					<p class="form-subtitle">Inicia sesión contra el entorno nube y reutiliza el token vigente.</p>
				</div>
				<form method="post" class="form-stack">
					<input type="hidden" name="action" value="login">
					<div class="field">
						<label for="login_email">Email</label>
						<input type="email" class="control" id="login_email" name="login_email" placeholder="juan@example.com" required>
					</div>
					<div class="field">
						<label for="login_password">Contraseña</label>
						<input type="password" class="control" id="login_password" name="login_password" placeholder="Contraseña" required>
					</div>
					<button type="submit" class="action-btn accent-blue">Obtener</button>
				</form>
			</div>
		</div>
	</div>
</div>

<script>
	document.querySelectorAll('.copy-btn').forEach(function (btn) {
		btn.addEventListener('click', function (event) {
			event.preventDefault();
			var targetId = this.getAttribute('data-target');
			var element = document.getElementById(targetId);
			if (!element) {
				return;
			}
			var text = element.innerText.trim();
			if (!navigator.clipboard) {
				var textarea = document.createElement('textarea');
				textarea.value = text;
				document.body.appendChild(textarea);
				textarea.select();
				document.execCommand('copy');
				document.body.removeChild(textarea);
			} else {
				navigator.clipboard.writeText(text).catch(function () {
					console.warn('No se pudo copiar el token al portapapeles.');
				});
			}
			this.textContent = 'Copiado';
			var button = this;
			setTimeout(function () {
				button.textContent = 'Copiar token';
			}, 2000);
		});
	});
</script>
</body>
</html>