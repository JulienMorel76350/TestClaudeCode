package com.veoneer.logisticinventoryapp.core.domain.use_case.feeder

import com.veoneer.logisticinventoryapp.core.domain.ValidationResult
import javax.inject.Inject

/**
 * UseCase générique pour valider une quantité numérique
 *
 * @param input Valeur à valider
 * @param maxValue Valeur maximale autorisée (optionnel)
 * @param minValue Valeur minimale autorisée (défaut: 1)
 */
class ValidateQuantityUseCase @Inject constructor() {

    operator fun invoke(
        input: String,
        maxValue: Int? = null,
        minValue: Int = 1
    ): ValidationResult {
        return when {
            input.isBlank() ->
                ValidationResult.Error("La quantité ne peut pas être vide")

            !input.matches(Regex("^\\d+$")) ->
                ValidationResult.Error("Seuls les chiffres sont autorisés")

            input.toIntOrNull() == null ->
                ValidationResult.Error("Valeur numérique invalide")

            input.toInt() < minValue ->
                ValidationResult.Error("La quantité doit être supérieure ou égale à $minValue")

            maxValue != null && input.toInt() > maxValue ->
                ValidationResult.Error("La quantité ne peut pas dépasser $maxValue")

            else -> ValidationResult.Success(input.toInt())
        }
    }
}