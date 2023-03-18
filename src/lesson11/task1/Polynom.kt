@file:Suppress("UNUSED_PARAMETER")

package lesson11.task1

import lesson4.task1.polynom
import ru.spbstu.wheels.NullableMonad.filter
import ru.spbstu.wheels.defaultCompareTo
import ru.spbstu.wheels.toRecordString
import java.sql.PseudoColumnUsage
import kotlin.math.pow
import kotlin.math.max
import kotlin.math.min

/**
 * Класс "полином с вещественными коэффициентами".
 *
 * Общая сложность задания -- средняя, общая ценность в баллах -- 16.
 * Объект класса -- полином от одной переменной (x) вида 7x^4+3x^3-6x^2+x-8.
 * Количество слагаемых неограничено.
 *
 * Полиномы можно складывать -- (x^2+3x+2) + (x^3-2x^2-x+4) = x^3-x^2+2x+6,
 * вычитать -- (x^3-2x^2-x+4) - (x^2+3x+2) = x^3-3x^2-4x+2,
 * умножать -- (x^2+3x+2) * (x^3-2x^2-x+4) = x^5+x^4-5x^3-3x^2+10x+8,
 * делить с остатком -- (x^3-2x^2-x+4) / (x^2+3x+2) = x-5, остаток 12x+16
 * вычислять значение при заданном x: при x=5 (x^2+3x+2) = 42.
 *
 * В конструктор полинома передаются его коэффициенты, начиная со старшего.
 * Нули в середине и в конце пропускаться не должны, например: x^3+2x+1 --> Polynom(1.0, 2.0, 0.0, 1.0)
 * Старшие коэффициенты, равные нулю, игнорировать, например Polynom(0.0, 0.0, 5.0, 3.0) соответствует 5x+3
 */
class Polynom(vararg coeffs: Double) {
    private var current = coeffs
    init {
        if (current.isNotEmpty()) {
            val firstNotZero = current.indexOfFirst { it != 0.0 }
            if (firstNotZero != -1) {
                current = current.copyOfRange(firstNotZero, current.size)
                current.reverse()
            }
        } else {
            current = doubleArrayOf(0.0)
        }
    }


    /**
     * Геттер: вернуть значение коэффициента при x^i
     */
    fun coeff(i: Int): Double = current.getOrNull(i) ?: throw NoSuchElementException()

    /**
     * Расчёт значения при заданном x
     */
    fun getValue(x: Double): Double = current.indices.sumOf { x.pow(it) * coeff(it) }
    /**
     * Степень (максимальная степень x при ненулевом слагаемом, например 2 для x^2+x+1).
     *
     * Степень полинома с нулевыми коэффициентами считать равной 0.
     * Слагаемые с нулевыми коэффициентами игнорировать, т.е.
     * степень 0x^2+0x+2 также равна 0.
     */
    fun degree(): Int = max(0, current.size - 1)

    /**
     * Сложение
     */
    operator fun plus(other: Polynom): Polynom {
        val result = DoubleArray(maxOf(current.size, other.current.size), { 0.0 })
        for (i in 0 until maxOf(current.size, other.current.size)) {
            if (i < current.size) result[i] += current[i]
            if (i < other.current.size) result[i] += other.current[i]
        }
        return Polynom(*result.reversed().toDoubleArray())
    }

    /**
     * Смена знака (при всех слагаемых)
     */
    operator fun unaryMinus(): Polynom {
        val result = DoubleArray(current.size) {0.0}
        for (i in current.indices) result[i] = -current[i]
        return Polynom(*result.reversed().toDoubleArray())
    }

    /**
     * Вычитание
     */
    operator fun minus(other: Polynom): Polynom = plus(other.unaryMinus())

    /**
     * Умножение
     */
    operator fun times(other: Polynom): Polynom {
        val result = DoubleArray((current.size + other.current.size), {0.0})
        for (first in 0 until current.size) {
            for (second in 0 until other.current.size) {
                result[first + second] += current[first] * other.current[second]
            }
        }
        return Polynom(*result.reversed().toDoubleArray())
    }

    /**
     * Деление
     *
     * Про операции деления и взятия остатка см. статью Википедии
     * "Деление многочленов столбиком". Основные свойства:
     *
     * Если A / B = C и A % B = D, то A = B * C + D и степень D меньше степени B
     */
    operator fun div(other: Polynom): Polynom {
        fun oneDiv(first: Polynom, second: Polynom): Triple<Polynom, Double, Int> {
            val firstDiv = first.current.toList()
            val secondDiv = (List(first.degree() - second.degree()) {0.0} + second.current.toList()).toMutableList()
            val result = MutableList(first.degree()) {0.0}
            for (i in secondDiv.indices) {
                secondDiv[i] = secondDiv[i] * (firstDiv.last() / secondDiv.last())
            }
            for (i in 0 until secondDiv.size - 1) {
                result[i] = firstDiv[i] - secondDiv[i]
            }
            return Triple(Polynom(*result.reversed().toDoubleArray()), firstDiv.last() / other.coeff(other.degree()), first.degree() - second.degree())
        }
        var ost = this
        var res = MutableList(this.degree() - other.degree() + 1) {0.0}
        do {
            var currPol = oneDiv(ost, other)
            res[currPol.third] = currPol.second
            ost = currPol.first
            if (ost.current.contentEquals(doubleArrayOf(0.0))) break
        } while (ost.current.size >= other.current.size)
        return Polynom(*res.reversed().toDoubleArray())
    }

    /**
     * Взятие остатка
     */
    operator fun rem(other: Polynom): Polynom {
        fun oneDiv(first: Polynom, second: Polynom): Triple<Polynom, Double, Int> {
            val firstDiv = first.current.toList()
            val secondDiv = (List(first.degree() - second.degree()) {0.0} + second.current.toList()).toMutableList()
            val result = MutableList(first.degree()) {0.0}
            for (i in secondDiv.indices) {
                secondDiv[i] = secondDiv[i] * (firstDiv.last() / secondDiv.last())
            }
            for (i in 0 until secondDiv.size - 1) {
                result[i] = firstDiv[i] - secondDiv[i]
            }
            return Triple(Polynom(*result.reversed().toDoubleArray()), firstDiv.last() / other.coeff(other.degree()), first.degree() - second.degree())
        }
        var ost = this
        var res = MutableList(this.degree() - other.degree() + 1) {0.0}
        do {
            var currPol = oneDiv(ost, other)
            res[currPol.third] = currPol.second
            ost = currPol.first
            if (ost.current.contentEquals(doubleArrayOf(0.0))) return Polynom(0.0)
            if (ost.current.size < other.current.size) return ost
        } while (true)
    }

    /**
     * Сравнение на равенство
     */
    override fun equals(other: Any?): Boolean = other is Polynom && other.current.contentEquals(current)

    /**
     * Получение хеш-кода
     */
    override fun hashCode(): Int = TODO()
}

