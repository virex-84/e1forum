# e1forum
**Не официальный клиент форума [www.e1.ru](https://www.e1.ru/talk/forum/list.php?f=67)** 
Десктопная версия: [www.e1.ru/talk/forum](https://www.e1.ru/talk/forum)
Мобильная версия: [https://m.e1.ru/f/](https://m.e1.ru/f/)
 
 # Описание
Клиент представляет из себя виджет для чтения форума, а также автоматическое кеширование для работы в оффлайн.

# Особенности
- Клиент работает на **Android 4.1** и выше (**API 16+**).
- Не содержит рекламу
- Позволяет залогиниться
- Парсит содержимое сайта не загружая рекламу

# Разработка
- Android Studio 3.5.1
- material тема
- retrofit2 - работа с сетью
- jsoup - парсинга сайта
- gson
- ViewModel, LiveData - представление данных
- WorkManager - работа с базой данных в background (вставка/обновление), переодическая работа с сетью
- Room (Entity, DAO) - база данных
- glide - загрузка изображений


# История
Сайт е1 в 2013 году был продан [ПАО «ВымпелКом» (Билайн)](https://ekaterinburg.beeline.ru/about/about-beeline/) некоему  [Hearst Shkulev Digital Regional Network](http://www.hearst-shkulev-media.ru/about/history/)

24.10.2019 сайт полностью потерял свой индивидуальный дизайн, перейдя на однотипный дизайн новостного агрегатора [Hearst Shkulev Digital Regional Network](http://www.hearst-shkulev-media.ru/about/history/) разработанный [Charmer studio](https://charmerstudio.com/)

Немного о "Charmer studio": https://www.the-village.ru/village/business/svoemesto/152429-studiya-charmer