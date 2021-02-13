package si.blarc.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import si.blarc.R
import si.blarc.entity.Player
import si.blarc.env.Logger
import si.blarc.env.RandomString
import si.blarc.env.UIUtils.replaceFragment
import si.blarc.firebase.FirebaseUtils.checkForGameFirebase
import si.blarc.firebase.FirebaseUtils.addPlayerToGame
import si.blarc.ui.BaseViewModel
import java.security.SecureRandom

/***
 * This is the first fragment a user sees. In this fragment user can choose offline or online mode.
 * @author blarc
 */
class MainFragment : Fragment() {
    private val logger: Logger = Logger()
    private val viewModel: BaseViewModel by activityViewModels()

    private lateinit var nicknameEditText: EditText
    private lateinit var gameIdEditText: EditText
    private lateinit var createBtn: Button
    private lateinit var joinBtn: Button

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nicknameEditText = view.findViewById(R.id.main_nickname_edit_text)
        gameIdEditText = view.findViewById(R.id.main_game_id_edit_text)
        createBtn = view.findViewById(R.id.main_create_btn)
        joinBtn = view.findViewById(R.id.main_join_btn)

        createBtn.setOnClickListener {

            if (nicknameEditText.text.isNotBlank()) {

                signAnonymously() {

                    val nickname = nicknameEditText.text.toString()
                    val player = Player(nickname, isAdmin = true, generated = false, viewModel.auth.uid!!)
                    viewModel.currentPlayer = player
                    viewModel.addPlayer(player)

                    val gameId = RandomString(4, SecureRandom(), RandomString.alphanumericUpper).nextString()
                    viewModel.gameId = gameId

                    addPlayerToGame(viewModel, player)
                            .addOnSuccessListener {
                                viewModel.database
                                        .child(gameId)
                                        .child(player.uuid)
                                        .onDisconnect()
                                        .removeValue()
                                logger.i("Created new game with id: $gameId")
                            }
                            .addOnFailureListener {
                                logger.e("Game was not created!", it)
                            }

                    replaceFragment(requireActivity(), LobbyFragment::class.java)

                }
            }
        }

        joinBtn.setOnClickListener {

            if (nicknameEditText.text.isNotBlank() && gameIdEditText.text.isNotBlank()) {

                signAnonymously() {

                    val nickname = nicknameEditText.text.toString()
                    val player = Player(nickname, isAdmin = false, generated = false, viewModel.auth.uid!!)
                    viewModel.currentPlayer = player
                    viewModel.addPlayer(player)

                    val gameId = gameIdEditText.text.toString()
                    viewModel.gameId = gameId

                    checkForGameFirebase(viewModel)
                        .addOnSuccessListener {
                            if (it.exists()) {
                                logger.i("Game exists!")
                                addPlayerToGame(viewModel, player)
                                        .addOnSuccessListener {
                                            viewModel.database
                                                    .child(gameId)
                                                    .child(player.uuid)
                                                    .onDisconnect()
                                                    .removeValue()
                                            logger.i("Joined game with id: $gameId")
                                            replaceFragment(requireActivity(), LobbyFragment::class.java)
                                        }
                                        .addOnFailureListener {
                                            logger.e("Couldn't join the game!", it)
                                        }
                            }
                            else {
                                logger.i("Game does not exist!")
                            }
                        }
                }
            }
        }
    }

    private fun signAnonymously(doAfter: Runnable) {
        viewModel.auth.signInAnonymously()
            .addOnCompleteListener(requireActivity()) {
                if (it.isSuccessful) {
                    doAfter.run()
                } else {
                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }



}