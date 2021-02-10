package si.blarc.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import si.blarc.R
import si.blarc.env.UIUtils.replaceFragment

/***
 * This fragment serves a lobby for players before the game. It shows players in the lobby and
 * enables game admin to set some game options.
 * @author blarc
 */
class LobbyFragment : Fragment() {

    private lateinit var startBtn: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_lobby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        startBtn = view.findViewById(R.id.lobby_start_btn)
        startBtn.setOnClickListener { onStartClick() }

    }

    private fun onStartClick() {
        replaceFragment(requireActivity(), LeaderboardFragment::class.java)
    }
}