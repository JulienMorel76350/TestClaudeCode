package com.veoneer.logisticinventoryapp.core.domain.use_case.feeder

import javax.inject.Inject

/**
 * UseCase pour normaliser un code scanné vers le format Movex (10 caractères max)
 *
 * Logique :
 * 1. Détecte le format standard $XXXXXSXXXXXXXX → Extrait après "S"
 * 2. Sinon → Nettoie les caractères spéciaux
 * 3. Supprime les leading zeros
 * 4. Tronque à 10 caractères max
 */
class NormalizeMovexCodeUseCase @Inject constructor() {

    operator fun invoke(scannedCode: String): Result<String> {
        if (scannedCode.isBlank()) {
            return Result.failure(Exception("Le code scanné est vide"))
        }

        // Étape 1 : Détecter le format standard avec "S"
        val standardFormatRegex = Regex("\\$[A-Z]+S(\\d+)")
        val matchResult = standardFormatRegex.find(scannedCode)

        val cleanedCode = if (matchResult != null) {
            // Format standard détecté : extraire après "S"
            matchResult.groupValues[1]
        } else {
            // Format non-standard : nettoyer les caractères spéciaux
            scannedCode.replace(Regex("[^A-Za-z0-9]"), "")
        }

        if (cleanedCode.isEmpty()) {
            return Result.failure(Exception("Le code ne contient aucun caractère valide"))
        }

        // Étape 2 : Supprimer les leading zeros (si le code est numérique)
        val withoutLeadingZeros = if (cleanedCode.all { it.isDigit() }) {
            cleanedCode.trimStart('0').ifEmpty { "0" }
        } else {
            cleanedCode
        }

        // Étape 3 : Tronquer à 10 caractères max
        val normalized = withoutLeadingZeros.take(10)

        return Result.success(normalized)
    }
}