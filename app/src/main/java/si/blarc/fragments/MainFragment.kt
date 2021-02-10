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
 * This is the first fragment a user sees. In this fragment user can choose offline or online mode.
 * @author blarc
 */
class MainFragment : Fragment() {

    private lateinit var offlineBtn: Button
    private lateinit var multiplayerBtn: Button

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        offlineBtn = view.findViewById(R.id.main_offline_mode_btn)
        offlineBtn.setOnClickListener { replaceFragment(requireActivity(), LobbyFragment::class.java)}

        multiplayerBtn = view.findViewById(R.id.main_multiplayer_btn)

    }



}