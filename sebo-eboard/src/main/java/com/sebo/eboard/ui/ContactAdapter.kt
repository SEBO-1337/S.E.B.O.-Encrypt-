package com.sebo.eboard.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sebo.eboard.R
import com.sebo.eboard.model.KeyboardContact

/**
 * Adapter für die Kontakt-Auswahl in der Tastatur
 */
class ContactAdapter(
    private val contacts: List<KeyboardContact>,
    private val activeContactId: String?,
    private val onContactSelected: (KeyboardContact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.contact_name)
        val statusText: TextView = view.findViewById(R.id.contact_status)
        val activeIndicator: TextView = view.findViewById(R.id.active_indicator)

        fun bind(contact: KeyboardContact) {
            nameText.text = contact.name

            // Zeige Status (SessionKey verfügbar oder nicht)
            statusText.text = if (contact.hasSessionKey) {
                "🔑 SessionKey verfügbar"
            } else {
                "⚠️ Kein SessionKey - bitte App öffnen"
            }

            // Zeige aktiven Kontakt
            activeIndicator.visibility = if (contact.id == activeContactId) {
                View.VISIBLE
            } else {
                View.GONE
            }

            itemView.setOnClickListener {
                onContactSelected(contact)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position])
    }

    override fun getItemCount() = contacts.size
}

