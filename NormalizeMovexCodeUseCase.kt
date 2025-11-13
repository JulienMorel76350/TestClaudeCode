package com.veoneer.logisticinventoryapp.core.domain.use_case.feeder

import javax.inject.Inject

/**
 * UseCase pour normaliser un code scanné vers le format Movex (10 caractères max)
 *
 * Logique :
 * 1. Nettoyer les caractères spéciaux ($, -, !, etc.)
 * 2. Séparer PRÉFIXE (lettres) et NUMÉRO DE SÉRIE (chiffres à la fin)
 * 3. Supprimer les leading zeros du numéro de série
 * 4. Recombiner préfixe + numéro
 * 5. Si > 10 caractères → Tronquer le PRÉFIXE pour préserver le numéro de série unique
 *
 * Exemples :
 * - $TIDBXW00009583 → TIDBXW9583 (10 chars)
 * - $TIDBXW00005678 → TIDBXW5678 (10 chars)
 * - VERYLONGPREFIX9583 → VERYLO9583 (10 chars, préfixe tronqué)
 */
class NormalizeMovexCodeUseCase @Inject constructor() {

    operator fun invoke(scannedCode: String): Result<String> {
        if (scannedCode.isBlank()) {
            return Result.failure(Exception("Le code scanné est vide"))
        }

        // Étape 1 : Nettoyer les caractères spéciaux
        val cleanedCode = scannedCode.replace(Regex("[^A-Za-z0-9]"), "")

        if (cleanedCode.isEmpty()) {
            return Result.failure(Exception("Le code ne contient aucun caractère valide"))
        }

        // Étape 2 : Séparer préfixe (lettres) et suffixe (numéro de série = chiffres à la fin)
        // Regex : [LETTRES][CHIFFRES]
        val regex = Regex("^([^0-9]*)([0-9]+)$")
        val matchResult = regex.find(cleanedCode)

        val normalized = if (matchResult != null) {
            val prefix = matchResult.groupValues[1] // Partie lettres (peut contenir lettres et autres)
            val serialNumber = matchResult.groupValues[2] // Chiffres à la fin

            // Étape 3 : Supprimer les leading zeros du numéro de série
            val trimmedSerial = serialNumber.trimStart('0').ifEmpty { "0" }

            // Étape 4 : Recombiner
            val combined = prefix + trimmedSerial

            // Étape 5 : Si > 10 caractères, tronquer le préfixe pour préserver le numéro de série
            if (combined.length > 10) {
                val maxPrefixLength = 10 - trimmedSerial.length
                if (maxPrefixLength > 0) {
                    // Garder autant de préfixe que possible + numéro complet
                    prefix.take(maxPrefixLength) + trimmedSerial
                } else {
                    // Le numéro de série seul dépasse 10 caractères
                    trimmedSerial.take(10)
                }
            } else {
                combined
            }
        } else {
            // Format non reconnu (pas de chiffres à la fin, ou que des chiffres, etc.)
            // Cas particulier : si que des chiffres, supprimer les leading zeros
            if (cleanedCode.all { it.isDigit() }) {
                cleanedCode.trimStart('0').ifEmpty { "0" }.take(10)
            } else {
                // Autre format : tronquer simplement à 10
                cleanedCode.take(10)
            }
        }

        return Result.success(normalized)
    }
}