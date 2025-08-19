package org.softsuave.bustlespot.tracker.ui.model

data class DropDownSelectionData<T>(
    val title: String,
    val onSearchText: (String) -> Unit,
    val inputText: String,
    val dropDownList: List<T>,
    val onItemClick: (T) -> Unit,
    val displayText: (T) -> String,
    val isSelectedItem: (T, T?) -> Boolean,
    val isEnabled: Boolean = true,
    val onDropDownClick: () -> Unit = {},
    val onNoOptionClick: () -> Unit = {},
    val error: String? = null,
    val selectedItem: T? = null,
    val isSelected: Boolean = false,
    val onDismissClick: () -> Unit = {},
    val readOnly: Boolean = false
)