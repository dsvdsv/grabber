#grabber

###Краткое описание 
После запуска программа, читает содежимое директории in, в соотвествии с поставленной задачей собирает содержащиеся в файлах ссылки, и закачивает ресурсы в директорию out
Для упращения я не стал явно реализовывать повторную отправку запроса, так как:

- Она есть в Akka Http, по умолчанию 5 попыток, через конфиг можно менять
- NoHttpResponseException не из jdk, помоему из apache common, такой зависимости у проекта нет
- Нет записи различных метаданных в файл только содержимое ресурса
- Нет разбиения на 100 документов
 
###Запуск
```
sbt clean run
```

###Тестирование
команда
```
sbt clean test
```
выполнит e2e тест из AppSpec. 

###Проблемы

- Все операции (пересылка сообщений и io) выполняются в одном дефолтном пуле потоков, все io операции следует делать на отдельном пуле
- Параметры операций дефолтные или взяты с потолка, в реальной системе их следует подобрать
- Быть может стоит сделать кластер с ActorSubscriber и раскидывать url по кластеру, так же в таком случае нужно что то решить с тем как и куда мы пишим содержимое документов 