<?php
//请勿在生产环境启用debug功能，可能导致敏感信息泄漏
$debug = false;
if (!$debug) {
    //抑制生产环境下的PHP错误，避免引起敏感信息泄漏
    error_reporting(0);
}
//数据库类型，不可更改
$db_type = "mysql";
//数据库IP地址
$db_host = "127.0.0.1";
//数据库端口
$db_port = 3306;
//数据库库名
$db_database = "minecaptcha";
//数据库表前缀
$db_tableprefix = "minecaptcha_";
//数据库用户名
$db_user = "root";
//数据库密码
$db_pass = "sunnyside666";
//请勿修改DSN
$db_dsn = "$db_type:host=$db_host;dbname=$db_database";

//Google reCaptcha地址，默认使用国内源
$recaptcha_host = "https://recaptcha.net/recaptcha/api/siteverify";
//Google reCaptcha SITE_KEY，填写你从谷歌验证码管理员平台申请的SITE KEY，需和recaptcha.html中保持一致
$recaptcha_sitekey = "%SITE_KEY%";
//Google reCaptcha SECRET_KEY，填写你从谷歌验证码管理员平台申请的SECRET KEY，请勿泄露
$recaptcha_secret = "%SECERT_KEY%";

//接收参数
$data_user = $_POST['username'];
$data_captchaToken = $_POST['recaptcha_response'];

//null/empty 检查
if ($data_user == null || $data_user == "" || $data_captchaToken == null || $data_captchaToken == "")
    die("哼，肮脏的黑客!");
//超长检查
if (mb_strlen($data_user) > 250)
    die("想搞我？洗洗睡吧！");
//在这里其实不用做特殊处理，后面咱们用PDO预处理语句，不用考虑XSS注入
$captcha_response = send_post($recaptcha_host, array(
    'secret' => $recaptcha_secret,
    'response' => $data_captchaToken
));
if ($captcha_response == null || $captcha_response == "")
    die("服务器与Google reCaptcha通信失败");
$decoded_captcha_response = json_decode($captcha_response, true);
if ($decoded_captcha_response == null || $decoded_captcha_response == "")
    die("内部错误，响应处理失败");
if ($decoded_captcha_response['success'] != true) {
    if (!$debug) {
        die("验证失败");
    } else {
        var_dump($decoded_captcha_response);
        die("验证失败");
    }
}
if ($decoded_captcha_response['score'] < 0.8) {
    if (!$debug) {
        die("看起来你是很可疑的机器人，请重试");
    } else {
        var_dump($decoded_captcha_response);
        die("看起来你是很可疑的机器人，请重试");
    }
}
//验证通过，处理MySQL数据
try {
    $dbh_pdo = new PDO($db_dsn, $db_user, $db_pass); //初始化一个PDO对象
    createTables($dbh_pdo, $db_tableprefix);
    setPlayerPassTheVerify($dbh_pdo, $db_tableprefix, $data_user, getUserIp(), $data_captchaToken);
    echo("<html>
<head>
<title>成功</title>
<meta charset=\"utf-8\"/>
</head>
<body>
<script>
alert(\"验证已完成\");
//window.location.href=\"about:blank\";
    </script>
    <h1>现在可以关闭此页面了</h1>
    </body>
    </html>
    ");
} catch (PDOException $e) {
    if (!$debug) {
        die ("与数据库连接失败");
    } else {
        echo str_replace($e->getTraceAsString(), "\n", "<br />");
        die ("与数据库连接失败: " . $e->getMessage() . "<br/>");
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
            die("操作执行失败：没有预期的处理结果(" . $stmt->columnCount() . ")");
        }
        $dbh->commit();
    } catch (Exception $e) {
        $dbh->rollBack();
        if (!$debug) {
            die("数据库执行失败");
        } else {
            echo str_replace($e->getTraceAsString(), "\n", "<br />");
            die ("SQL执行错误: " . $e->getMessage() . "<br/>");
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
            die("数据库执行失败");
        } else {
            echo str_replace($e->getTraceAsString(), "\n", "<br />");
            die ("SQL执行错误: " . $e->getMessage() . "<br/>");
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
            'timeout' => 10 * 60 // 超时时间（单位:s），服务器网络不好的话请调高这个
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