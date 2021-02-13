package si.blarc.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import si.blarc.ui.BaseViewModel
import si.blarc.R
import si.blarc.entity.Player
import si.blarc.env.Logger
import si.blarc.env.UIUtils.replaceFragment
import si.blarc.firebase.FirebaseUtils.addGameValueListener
import si.blarc.firebase.FirebaseUtils.addPlayerToGame
import si.blarc.ui.PlayersAdapter

/***
 * This fragment serves a lobby for players before the game. It shows players in the lobby and
 * enables game admin to set some game options.
 * @author blarc
 */
class LobbyFragment : Fragment(), ValueEventListener {
    private val logger: Logger = Logger()

    private val viewModel: BaseViewModel by activityViewModels()

    private lateinit var gameIdTextView: TextView
    private lateinit var playersRecyclerView: RecyclerView
    private lateinit var addEditText: EditText
    private lateinit var startBtn: Button
    private lateinit var addButton: Button

    private lateinit var playersAdapter: PlayersAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gameIdTextView = view.findViewById(R.id.lobby_game_id_text_view)
        gameIdTextView.text = viewModel.gameId

        startBtn = view.findViewById(R.id.lobby_start_btn)
        startBtn.setOnClickListener { onStartClick() }

        addEditText = view.findViewById(R.id.lobby_add_edit_text)
        addButton = view.findViewById(R.id.lobby_add_button)
        addButton.setOnClickListener {
            val player = Player(addEditText.text.toString(), isAdmin = false, generated = true)
            viewModel.addPlayer(player)
            addPlayerToGame(viewModel, player)
        }

        playersRecyclerView = view.findViewById(R.id.lobby_players_recycler_view)
        playersRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        playersAdapter = PlayersAdapter(viewModel.getPlayers(), requireContext())
        playersRecyclerView.adapter = playersAdapter

        viewModel.players.observe(viewLifecycleOwner) {
            playersAdapter.setPlayers(viewModel.getPlayers())
        }

        addGameValueListener(viewModel, this)

    }

    private fun onStartClick() {
        replaceFragment(requireActivity(), LeaderboardFragment::class.java)
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        val value = snapshot.value
        logger.i("Data: " + value.toString())
    }

    override fun onCancelled(error: DatabaseError) {
        logger.i("On cancelled!")
    }
}