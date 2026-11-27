package ru.pfc.shared.data.seed

import ru.pfc.shared.model.Category
import ru.pfc.shared.model.CategoryKind
import kotlin.time.Instant

/**
 * Готовый набор категорий с одним уровнем подкатегорий (R13) и отметкой «обязательная» (R14).
 * Подкатегория наследует вид и отметку родителя. Идентификаторы постоянные: на них ссылается словарь магазинов.
 */
internal fun defaultCategories(now: Instant): List<Category> = DEFAULT_CATEGORY_TREE.flatMap { parent ->
    val parentCategory = Category(
        id = parent.id,
        name = parent.name,
        kind = parent.kind,
        icon = parent.icon,
        isMandatory = parent.isMandatory,
        updatedAt = now,
    )
    listOf(parentCategory) + parent.children.map { (childKey, childName) ->
        Category(
            id = "${parent.id}.$childKey",
            name = childName,
            kind = parent.kind,
            parentId = parent.id,
            isMandatory = parent.isMandatory,
            updatedAt = now,
        )
    }
}

private class DefaultCategory(
    key: String,
    val name: String,
    val icon: String,
    val kind: CategoryKind = CategoryKind.EXPENSE,
    val isMandatory: Boolean = false,
    val children: List<Pair<String, String>> = emptyList(),
) {
    val id = "default.$key"
}

private val DEFAULT_CATEGORY_TREE = listOf(
    DefaultCategory(
        "groceries", "Продукты", "shopping_cart", isMandatory = true,
        children = listOf("supermarkets" to "Супермаркеты", "delivery" to "Доставка продуктов"),
    ),
    DefaultCategory(
        "cafe", "Кафе и рестораны", "restaurant",
        children = listOf(
            "restaurants" to "Рестораны",
            "coffee" to "Кофе",
            "fastfood" to "Фастфуд",
            "delivery" to "Доставка еды",
        ),
    ),
    DefaultCategory(
        "transport", "Транспорт", "directions_bus", isMandatory = true,
        children = listOf(
            "public" to "Общественный транспорт",
            "taxi" to "Такси",
            "fuel" to "Топливо",
            "parking" to "Парковка",
        ),
    ),
    DefaultCategory(
        "housing", "Жильё", "home", isMandatory = true,
        children = listOf("rent" to "Аренда", "utilities" to "Коммунальные услуги", "repair" to "Ремонт"),
    ),
    DefaultCategory(
        "connection", "Связь и интернет", "wifi", isMandatory = true,
        children = listOf("mobile" to "Мобильная связь", "internet" to "Домашний интернет"),
    ),
    DefaultCategory(
        "health", "Здоровье", "medical_services", isMandatory = true,
        children = listOf("pharmacy" to "Аптеки", "doctors" to "Врачи и анализы"),
    ),
    DefaultCategory("loans", "Кредиты", "account_balance", isMandatory = true),
    DefaultCategory("clothes", "Одежда и обувь", "checkroom"),
    DefaultCategory("household", "Товары для дома", "chair"),
    DefaultCategory("subscriptions", "Подписки", "subscriptions"),
    DefaultCategory(
        "entertainment", "Развлечения", "theater_comedy",
        children = listOf("events" to "Кино и концерты", "hobby" to "Хобби", "games" to "Игры"),
    ),
    DefaultCategory("beauty", "Красота и уход", "spa"),
    DefaultCategory("children", "Дети", "child_care"),
    DefaultCategory("pets", "Животные", "pets"),
    DefaultCategory("education", "Образование", "school"),
    DefaultCategory("gifts", "Подарки", "redeem"),
    DefaultCategory("travel", "Путешествия", "flight"),
    DefaultCategory("fees", "Комиссии банков", "receipt_long"),
    DefaultCategory("other_expense", "Другое", "more_horiz"),
    DefaultCategory("salary", "Зарплата", "payments", kind = CategoryKind.INCOME),
    DefaultCategory("side_income", "Подработка", "work", kind = CategoryKind.INCOME),
    DefaultCategory("cashback", "Кешбэк и проценты", "savings", kind = CategoryKind.INCOME),
    DefaultCategory("gifts_income", "Подарки", "card_giftcard", kind = CategoryKind.INCOME),
    DefaultCategory("other_income", "Другие доходы", "more_horiz", kind = CategoryKind.INCOME),
)
