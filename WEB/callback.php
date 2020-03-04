<?php
//Debug option, enable this will shown error messages, but may cause data leaking.
//Only enable it when you actually need.
$debug = false;
if (!$debug) {
    error_reporting(0);
}

//================DATABASE================//
//Database Host
$db_host = "127.0.0.1";
//Database Port
$db_port = 3306;
//Database Database
$db_database = "minecaptcha";
//Database Prefix
$db_tableprefix = "minecaptcha_";
//Database User
$db_user = "root";
//Database Password
$db_pass = "12345678";
//DSN, DO NOT TOUCH IT.
$db_dsn = "mysql:host=$db_host;dbname=$db_database";

//You site key from Google reCaptcha
$recaptcha_sitekey = "%SITE_KEY%";
//You secret key from Google reCaptcha
$recaptcha_secret = "%SECERT_KEY%";

$recaptcha_host = "https://www.google.com/recaptcha/api/siteverify";
//For chinese user, there is google official mirror:
//$recaptcha_host = "https://recaptcha.net/recaptcha/api/siteverify";





//================CORE PART================//

$data_user = $_POST['username'];
$data_captchaToken = $_POST['recaptcha_response'];


if ($data_user == null || $data_user == "" || $data_captchaToken == null || $data_captchaToken == "")
    die("No, you need submit something to there");

if (mb_strlen($data_user) > 250)
    die("Bump, dirty hacker");

$captcha_response = send_post($recaptcha_host, array(
    'secret' => $recaptcha_secret,
    'response' => $data_captchaToken
));
if ($captcha_response == null || $captcha_response == "")
    die("Cannot connect to Google reCaptcha server");
$decoded_captcha_response = json_decode($captcha_response, true);
if ($decoded_captcha_response == null || $decoded_captcha_response == "")
    die("Internal Error");
if ($decoded_captcha_response['success'] != true) {
    if (!$debug) {
        die("Failed to verify");
    } else {
        var_dump($decoded_captcha_response);
        die("Failed to verify");
    }
}
if ($decoded_captcha_response['score'] < 0.8) {
    if (!$debug) {
        die("Please retry, because seems you are robot.");
    } else {
        var_dump($decoded_captcha_response);
        die("Please retry, because seems you are robot.");
    }
}

try {
    $dbh_pdo = new PDO($db_dsn, $db_user, $db_pass);
    createTables($dbh_pdo, $db_tableprefix);
    setPlayerPassTheVerify($dbh_pdo, $db_tableprefix, $data_user, getUserIp(), $data_captchaToken);
    //================ SUCCESS WEB PAGE================//
    echo("<html>
<head>
<title>Success</title>
<meta charset=\"utf-8\"/>
</head>
<body>
<script>
alert(\"Successfully to verify your status, now you can back to game.\");
//window.location.href=\"about:blank\";
    </script>
    <h1>Now you can close this tab.</h1>
    </body>
    </html>
    ");
} catch (PDOException $e) {
    if (!$debug) {
        die ("Failed connect to database");
    } else {
        echo str_replace($e->getTraceAsString(), "\n", "<br />");
        die ("Failed connect to database: " . $e->getMessage() . "<br/>");
    }

}


function setPlayerPassTheVerify(PDO $dbh, $db_tableprefix, $playerName, $ipaddress, $captcha_token)
{
    global $debug;
    try {
        list($msec, $sec) = explode(' ', microtime());
        $msectime =  (float)sprintf('%.0f', (floatval($msec) + floatval($sec)) * 1000);
        $msectimes = substr($msectime,0,13);
        $dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $dbh->beginTransaction();
        $stmt = $dbh->prepare("INSERT INTO " . $db_tableprefix . "info" . " (username, ipaddress, createtime) VALUES (:username, :ipaddress, :createtime) ON DUPLICATE KEY UPDATE username=:username, ipaddress=:ipaddress, createtime=:createtime");
        $stmt->bindParam(":username", $playerName);
        $stmt->bindParam(":ipaddress", $ipaddress);
        $stmt->bindParam(":createtime", $msectimes);
        $stmt->execute();
        if ($stmt->columnCount() < 0) {
            $dbh->rollBack();
            die("Failed, no except response(" . $stmt->columnCount() . ")");
        }
        $dbh->commit();
    } catch (Exception $e) {
        $dbh->rollBack();
        if (!$debug) {
            die("Failed execute sql query");
        } else {
            echo str_replace($e->getTraceAsString(), "\n", "<br />");
            die ("Failed execute sql query: " . $e->getMessage() . "<br/>");
        }

    }
}

function createTables(PDO $dbh, $db_tableprefix)
{
    global $debug;
    try {
        $dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $dbh->beginTransaction();
        $dbh->exec("CREATE TABLE IF NOT EXISTS " . $db_tableprefix . "info" . "(
    username VARCHAR(255) PRIMARY KEY,
    ipaddress VARCHAR(255),
    createtime BIGINT(255)
)");
        $dbh->commit();
    } catch (Exception $e) {
        $dbh->rollBack();
        if (!$debug) {
            die("Failed execute sql query");
        } else {
            echo str_replace($e->getTraceAsString(), "\n", "<br />");
            die ("Failed execute sql query: " . $e->getMessage() . "<br/>");
        }
    }
}
function send_post($url, $post_data)
{
    $postdata = http_build_query($post_data);
    $options = array(
        'http' => array(
            'method' => 'POST',
            'header' => 'Content-type:application/x-www-form-urlencoded',
            'content' => $postdata,
            'timeout' => 10 * 60 // Timeout
        )
    );
    $context = stream_context_create($options);
    $result = file_get_contents($url, false, $context);
    return $result;
}

function getUserIp()
{
    if (!empty($_SERVER["HTTP_CLIENT_IP"])) {
        $cip = $_SERVER["HTTP_CLIENT_IP"];
    } elseif (!empty($_SERVER["HTTP_X_FORWARDED_FOR"])) {
        $cip = $_SERVER["HTTP_X_FORWARDED_FOR"];
    } elseif (!empty($_SERVER["REMOTE_ADDR"])) {
        $cip = $_SERVER["REMOTE_ADDR"];
    } else {
        $cip = "Error: Failed to get user IP address.";
    }
    return $cip;
}