package ru.pfc.shared.data.seed

/**
 * Популярный магазин и категория, которую получает новая покупка в нём (KTD10, AE2).
 * [names] — написания, которые встречаются в уведомлениях; ключ магазина из них строит нормализация в U13.
 * Правило пользователя важнее словаря (R8).
 */
data class DefaultMerchant(val names: List<String>, val categoryId: String)

/** Готовый словарь популярных магазинов РФ. Состав уточняется по собранным уведомлениям (Deferred to Implementation). */
val DefaultMerchants: List<DefaultMerchant> = listOf(
    merchant("default.groceries.supermarkets", "Пятёрочка", "Pyaterochka"),
    merchant("default.groceries.supermarkets", "Перекрёсток", "Perekrestok"),
    merchant("default.groceries.supermarkets", "Магнит", "Magnit"),
    merchant("default.groceries.supermarkets", "ВкусВилл", "VkusVill"),
    merchant("default.groceries.supermarkets", "Лента", "Lenta"),
    merchant("default.groceries.supermarkets", "Ашан", "Auchan"),
    merchant("default.groceries.supermarkets", "Дикси", "Dixy"),
    merchant("default.groceries.supermarkets", "Чижик", "Chizhik"),
    merchant("default.groceries.delivery", "Самокат", "Samokat"),
    merchant("default.groceries.delivery", "Яндекс Лавка", "Yandex Lavka"),
    merchant("default.cafe.delivery", "Яндекс Еда", "Yandex Eda"),
    merchant("default.cafe.coffee", "Шоколадница", "Shokoladnitsa"),
    merchant("default.cafe.coffee", "Cofix", "Кофикс"),
    merchant("default.cafe.coffee", "Coffee Like", "Кофе Лайк"),
    merchant("default.cafe.fastfood", "Вкусно — и точка", "Vkusno i tochka"),
    merchant("default.cafe.fastfood", "Бургер Кинг", "Burger King"),
    merchant("default.cafe.fastfood", "Ростикс", "Rostics"),
    merchant("default.transport.taxi", "Яндекс Go", "Yandex Go"),
    merchant("default.transport.public", "Мосметро", "Mosmetro"),
    merchant("default.transport.public", "Тройка", "Troika"),
    merchant("default.transport.fuel", "Лукойл", "Lukoil"),
    merchant("default.transport.fuel", "Роснефть", "Rosneft"),
    merchant("default.transport.fuel", "Газпромнефть", "Gazpromneft"),
    merchant("default.transport.fuel", "Татнефть", "Tatneft"),
    merchant("default.connection.mobile", "МТС", "MTS"),
    merchant("default.connection.mobile", "Билайн", "Beeline"),
    merchant("default.connection.mobile", "МегаФон", "MegaFon"),
    merchant("default.connection.mobile", "T2", "Tele2"),
    merchant("default.health.pharmacy", "Ригла", "Rigla"),
    merchant("default.health.pharmacy", "Аптеки 36,6", "36.6"),
    merchant("default.health.pharmacy", "Горздрав", "Gorzdrav"),
    merchant("default.health.pharmacy", "Здравсити", "Zdravcity"),
    merchant("default.subscriptions", "Кинопоиск", "Kinopoisk"),
    merchant("default.subscriptions", "Яндекс Плюс", "Yandex Plus"),
    merchant("default.subscriptions", "Okko", "Окко"),
    merchant("default.subscriptions", "Иви", "ivi"),
    merchant("default.subscriptions", "VK Музыка", "VK Music"),
    merchant("default.subscriptions", "Wink", "Винк"),
    merchant("default.household", "Fix Price", "Фикс Прайс"),
    merchant("default.household", "Лемана ПРО", "Lemana PRO"),
    merchant("default.beauty", "Золотое яблоко", "Goldapple"),
    merchant("default.beauty", "Лэтуаль", "Letu"),
    merchant("default.children", "Детский мир", "Detmir"),
)

private fun merchant(categoryId: String, vararg names: String) = DefaultMerchant(names.toList(), categoryId)
