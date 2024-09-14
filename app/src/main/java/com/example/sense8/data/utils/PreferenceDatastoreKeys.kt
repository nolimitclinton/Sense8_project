package com.example.sense8.data.utils

import androidx.datastore.preferences.core.booleanPreferencesKey
import com.example.sense8.utils.Constants

/**
 * Defines keys for accessing data in the preferences DataStore.
 */
object PreferenceDatastoreKeys {
    val USER_CONFIG = booleanPreferencesKey(name = Constants.USER_CONFIG)
}