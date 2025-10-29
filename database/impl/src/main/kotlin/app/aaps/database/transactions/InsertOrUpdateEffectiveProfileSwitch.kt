package app.aaps.database.transactions

import app.aaps.database.entities.EffectiveProfileSwitch

class InsertOrUpdateEffectiveProfileSwitch(private val effectiveProfileSwitch: EffectiveProfileSwitch) : Transaction<InsertOrUpdateEffectiveProfileSwitch.TransactionResult>() {

    override fun run(): TransactionResult {
        val result = TransactionResult()

        val current = database.effectiveProfileSwitchDao.findById(effectiveProfileSwitch.id)
        if (current == null) {
            database.effectiveProfileSwitchDao.insertNewEntry(effectiveProfileSwitch)
            result.inserted.add(effectiveProfileSwitch)
        } else {
            database.effectiveProfileSwitchDao.updateExistingEntry(effectiveProfileSwitch)
            result.updated.add(effectiveProfileSwitch)
        }
        return result
    }

    class TransactionResult {

        val inserted = mutableListOf<EffectiveProfileSwitch>()
        val updated = mutableListOf<EffectiveProfileSwitch>()
    }
}