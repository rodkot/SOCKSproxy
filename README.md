<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html lang="ru">
<head>
    <meta http-equiv="Content-Type" content="text/html;charset=utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Сети 2022, #5: SOCKS-прокси</title>
    <link rel="stylesheet" type="text/css" href="tasks.css">
</head>
<body>
<div class="task">
    <a id="toList" href="list.html">[к списку]</a>
    <h1>SOCKS-прокси</h1>
    <div>
        <ol>
        <li>Необходимо реализовать прокси-сервер, соответствующий стандарту SOCKS версии 5.</li>
        <li>В параметрах программе передаётся только порт, на котором прокси будет ждать входящих подключений от клиентов.</li>
        <li>Из трёх доступных в протоколе команд, обязательной является только реализация команды <code>1</code> (<i>establish a TCP/IP stream connection</i>)</li>
        <li>Поддержку аутентификации и IPv6-адресов реализовывать не требуется.</li>
        <li>Для реализации прокси использовать неблокирующиеся сокеты, работая с ними в рамках одного треда. Дополнительные треды использовать не допускается. Соответственно, никаких блокирующихся вызовов (кроме вызова селектора) не допускается.</li>
        <li>Прокси не должна делать предположений о том, какой протокол уровня приложений будет использоваться внутри перенаправляемого TCP-соединения. В частности, должна поддерживаться передача данных одновременно в обе стороны, а соединения должны закрываться аккуратно (только после того, как они больше не нужны).</li>
        <li>В приложении не должно быть холостых циклов ни в каких ситуациях. Другими словами, не должно быть возможно состояние программы, при котором неоднократно выполняется тело цикла, которое не делает ни одной фактической передачи данных за итерацию.</li>
        <li>Не допускается неограниченное расходование памяти для обслуживания одного клиента.</li>
        <li>Производительность работы через прокси не должна быть заметно хуже, чем без прокси. Для отслеживания корректности и скорости работы можно глядеть в Developer tools браузера на вкладку Network.</li>
        <li>Прокси должен поддерживать резолвинг доменных имён (значение <code>0x03</code> в поле <code>address</code>). Резолвинг тоже должен быть неблокирующимся. Для этого предлагается использовать следующий подход:
            <ul>
                <li>На старте программы создать новый UDP-сокет и добавить его в селектор на чтение</li>
                <li>Когда необходимо отрезолвить доменное имя, отправлять через этот сокет DNS-запрос A-записи на адрес рекурсивного DNS-резолвера</li>
                <li>В обработчике чтения из сокета обрабатывать случай, когда получен ответ на DNS-запрос, и продолжать работу с полученным адресом</li>
            </ul>
            Для получения <a href="https://stackoverflow.com/a/51844866/501399">адреса рекурсивного резолвера</a>, а также для <a href="https://javadoc.io/doc/dnsjava/dnsjava/latest/org/xbill/DNS/Message.html">формирования и парсинга DNS-сообщений</a> на Java предлагается использовать библиотеку <a href="http://www.dnsjava.org/">dnsjava</a> (для других языков найдите сами).
        </li>
        </ol>
    </div>
    <div>
        Для тестирования можно настроить любой Web-браузер на использование вашего прокси, и посещать любые веб-сайты, богатые контентом.
    </div>
    <br>
    <div>
    Описание протокола:
    <ol class="list-number">
        <li><a href="https://en.wikipedia.org/wiki/SOCKS">На английской Википедии</a></li>
        <li><a href="https://www.ietf.org/rfc/rfc1928.txt">SOCKS 5 RFC</a></li>
        <li><a href="socks.jpg">SOCKS для самых маленьких</a></li>
    </ol>
    </div>
<div>
    Баллов за задачу: 3
</div>
</div>
</body>
</html>
