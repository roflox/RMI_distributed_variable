Použité knihovny:
log4j2
apache-cli

Technologie:
MAVEN
Java RMI

Je třeba mít Java 11, doporučuji Oracle distribuci, ale funguje i na OpenJdk.

Stačí spustit nodeImpl-jar-with-dependencies.jar.

Příklad spuštění:

java -jar ./nodeImpl-jar-with-dependencies.jar -[přepínač]

Lokální přepínače:
-n --name                    Jméno objektu v RMI registrech. POVINNÉ.
-h --hostname                Ip adresa RMI serveru tohoto nodu. V případě, že se nezadá, pokusí se node pingout 8.8.8.8 a podle interfacu ze kterého to lze, pokud nelze nastaví localhost.
-p --port                    Port objektu v RMI registrech. POVINNÉ
-r --registryPort            Port RMI serveru tohoto nodu. POVINNÉ
-d --debug                   Debugovací režim.

Pro připojení k dalšímu nodu:
-t --target                  Jméno nodu, ke kterému se připojujete.
-A --targetRegistryAddress   IP adresa RMI registrů, ke je vystavený node, ke kterému se připojujete. Když není uvedený, použije se localhost.
-P --targetRegistryPort      Port RMI registrů cílového nodu. POVINNÝ při použití -t --target.
-w --waitTime                Doba čekání, když se nepovede připojit k cílovému nodu. Node počká x vteřin a pokusí se připojit znovu. Maximálně však 5x. Defaultní hodnota je 2 sekundy.

V případě použití jen lokálních přepínačů, se node považuje za Leadera a k nikomu se nepřipojuje.
Příkad:

java -jar ../target/NodeImpl-jar-with-dependencies.jar -n Node1 -p 50000 -r 1099

Příklad pro připojení k témuž nodu.

java -jar ../target/NodeImpl-jar-with-dependencies.jar -n Node2 -p 50001 -r 1100 -t Node1 -P 1099




Aplikace se spustí a v případě, že se vše spustí jak má uživatel má možnost přímo do konzole psát následující příkazy.

a add		pro přičtení 1 od proměnné "Variable"
s substract	pro odečtení 1 od proměnné "Variable"
r random	pro vložení náhodného čísla od proměnné "Variable"
w wipe		pro nastavení hodnoty 0 do proměnné "Variable"
d debug		pro přepnutí do debug módu
q quit		pro ukončení nodu
i info		pro vypsání údajů, které má node
e election	pro znovuzvolení vůdce
h help		pro vypsání těchto informací

Je možné zadat více příkazů najednou např "aaasiw" najednou provede 3x add 1x substract 1x info a nakonec 1x wipe.

V případě, že se odpojí nějaký node nic se neděje až do té doby, kdy se pomocí nějakého příkazu nezjistí, že daný node umřel. Poté se spraví topologie, a vyhodnotí se zdali je potřeba zvolit nového vůdce.

Vůdce vidí na všechny nody a delegujej jejich práci.

