package ru.pfc.shared.model

import kotlinx.datetime.LocalDate
import ru.pfc.shared.money.Money
import kotlin.time.Instant

enum class OperationType {
    EXPENSE,
    INCOME,
    TRANSFER,

    /** Начальный или фактический остаток счёта (R40). Не расход и не доход, в аналитику не входит. */
    ADJUSTMENT,
}

/** Куда движутся деньги перевода относительно счёта [Operation.accountId]. */
enum class TransferDirection { OUT, IN }

enum class OperationSource { MANUAL, VOICE, NOTIFICATION }

enum class OperationStatus { CONFIRMED, PENDING }

/** Почему операция ждёт ответа во «Входящих» и входит ли она до ответа в баланс (допущение A2). */
enum class PendingReason(val countsInBalance: Boolean) {
    NEW_MERCHANT(countsInBalance = true),
    UNPAIRED_TRANSFER(countsInBalance = true),
    CHOOSE_ACCOUNT(countsInBalance = true),
    OVER_LIMIT(countsInBalance = false),
    UNPARSED(countsInBalance = false),
    NO_ACCOUNT(countsInBalance = false),
}

/**
 * Операция по счёту (KTD7).
 *
 * [amount] всегда в валюте счёта. У расхода, дохода и перевода он положительный, направление задают [type] и [transferDirection].
 * У корректировки [amount] — разница со знаком, а [balanceAfter] — остаток счёта на момент корректировки:
 * баланс равен остатку последней корректировки плюс операции строго позже неё.
 * У неразобранного уведомления (UNPARSED) тип и сумма могут быть неизвестны.
 * [originalAmount] — сумма в иностранной валюте, как её прислал банк (A13). Пока пользователь не указал,
 * сколько списано в валюте счёта, [amount] нет и операция ждёт во «Входящих» как UNPARSED.
 *
 * Перевод: [accountId] — свой счёт, с которого (OUT) или на который (IN) идут деньги;
 * [transferAccountId] — другой свой счёт или null, если вторая сторона вне приложения.
 * Перевод между своими счетами записывается одной операцией OUT со счёта списания.
 *
 * [occurredAt] — момент операции, [localDate] — дата по местному времени на этот момент (KTD8).
 */
data class Operation(
    override val id: String,
    val type: OperationType?,
    val amount: Money?,
    val originalAmount: Money? = null,
    val accountId: String?,
    val occurredAt: Instant,
    val localDate: LocalDate,
    val status: OperationStatus,
    val pendingReason: PendingReason? = null,
    val source: OperationSource,
    val categoryId: String? = null,
    val merchant: String? = null,
    val comment: String? = null,
    val bank: Bank? = null,
    val transferDirection: TransferDirection? = null,
    val transferAccountId: String? = null,
    /** Покупка, которую возвращает эта операция. */
    val refundOfId: String? = null,
    val balanceAfter: Money? = null,
    override val updatedAt: Instant,
    override val deletedAt: Instant? = null,
) : SyncedEntity {
    init {
        require((status == OperationStatus.PENDING) == (pendingReason != null)) {
            "Причина ожидания есть только у операции со статусом PENDING"
        }
        require(accountId != null || pendingReason == PendingReason.UNPARSED || pendingReason == PendingReason.NO_ACCOUNT) {
            "Без счёта бывают только операции UNPARSED и NO_ACCOUNT"
        }
        require((type != null && amount != null) || pendingReason == PendingReason.UNPARSED) {
            "Без типа или суммы бывает только неразобранное уведомление UNPARSED"
        }
        require(amount == null || type == OperationType.ADJUSTMENT || amount.minorUnits > 0) {
            "Сумма расхода, дохода и перевода положительна"
        }
        require(originalAmount == null || originalAmount.minorUnits > 0) {
            "Сумма в иностранной валюте положительна"
        }
        require(originalAmount == null || amount == null || originalAmount.currency != amount.currency) {
            "Исходная сумма хранится, только если её валюта отличается от валюты счёта"
        }
        require((type == OperationType.ADJUSTMENT) == (balanceAfter != null)) {
            "Остаток на момент операции хранит только корректировка"
        }
        require((type == OperationType.TRANSFER) == (transferDirection != null)) {
            "Направление есть только у перевода"
        }
        require(transferAccountId == null || transferDirection == TransferDirection.OUT) {
            "Перевод между своими счетами записывается со стороны списания"
        }
        require(transferAccountId == null || transferAccountId != accountId) {
            "Перевод на тот же счёт"
        }
    }

    /** Входит ли операция в баланс и «можно тратить» до ответа во «Входящих» (A2). */
    val countsInBalance: Boolean get() = pendingReason?.countsInBalance ?: true
}
