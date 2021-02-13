package si.blarc.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import si.blarc.R
import si.blarc.entity.Player

class PlayersAdapter(private var players: ArrayList<Player>, private val context: Context) : RecyclerView.Adapter<PlayersAdapter.PlayerHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerHolder {
        return PlayerHolder(LayoutInflater.from(context).inflate(R.layout.lobby_player_holder, parent, false))
    }

    override fun getItemCount(): Int {
        return players.size
    }

    fun setPlayers(players: ArrayList<Player>) {
        this.players.clear()
        this.players.addAll(players)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: PlayerHolder, position: Int) {
        val player = players[position]
        holder.bindPlayer(player)
    }

    inner class PlayerHolder(private val view: View) : RecyclerView.ViewHolder(view) {

        fun bindPlayer(player: Player) {
            val nicknameTextView: TextView = view.findViewById(R.id.lobby_player_holder_nickname)
            val pointsTextView: TextView = view.findViewById(R.id.lobby_player_holder_points)

            nicknameTextView.text = player.nickname
            pointsTextView.text = player.points.toString()
        }
    }
}