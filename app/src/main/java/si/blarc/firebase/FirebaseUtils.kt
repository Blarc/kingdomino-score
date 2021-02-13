package si.blarc.firebase

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import si.blarc.entity.Player
import si.blarc.env.Constants
import si.blarc.env.Logger
import si.blarc.ui.BaseViewModel

object FirebaseUtils {
    private val logger: Logger = Logger()

    fun addPlayerToGame(viewModel: BaseViewModel, player: Player): Task<Void> {
        return viewModel.database
            .child(viewModel.gameId!!)
            .child(player.uuid)
            .setValue(player)
    }

    fun deleteCurrentPlayerFromFirebase(viewModel: BaseViewModel): Task<Void> {
        return viewModel.auth.currentUser!!.delete()
    }

    fun removeCurrentPlayerFromGame(viewModel: BaseViewModel): Task<Void> {
        return removePlayerFromGame(viewModel, viewModel.currentPlayer!!)
    }

    fun removePlayerFromGame(viewModel: BaseViewModel, player: Player): Task<Void> {
        return viewModel.database
            .child(viewModel.gameId!!)
            .child(player.uuid)
            .setValue(null)
    }

    fun checkForGameFirebase(viewModel: BaseViewModel) : Task<DataSnapshot> {
        return viewModel.database
            .child(viewModel.gameId!!)
            .get()
    }

    fun addGameValueListener(viewModel: BaseViewModel, valueChangeListener: ValueEventListener) {
        viewModel.database
            .child(viewModel.gameId!!)
            .addValueEventListener(valueChangeListener)
    }

    fun removeGameValueListener(viewModel: BaseViewModel, valueChangeListener: ValueEventListener) {
        viewModel.database
                .child(viewModel.gameId!!)
                .removeEventListener(valueChangeListener)
    }

    fun setNewGameAdmin(viewModel: BaseViewModel): Task<Void> {
        val findFirst = viewModel.players.value?.stream()?.filter {
            !it.isAdmin && !it.generated && it.uuid != viewModel.currentPlayer?.uuid
        }?.findFirst()

        return if (findFirst!!.isPresent) {
            setNewGameAdmin(viewModel, findFirst.get())
        } else {
            removeGame(viewModel)
        }
    }

    fun setNewGameAdmin(viewModel: BaseViewModel, player: Player): Task<Void> {
        return viewModel.database
                .child(viewModel.gameId!!)
                .child(player.uuid)
                .child("isAdmin")
                .setValue(true)
    }

    fun removeGame(viewModel: BaseViewModel): Task<Void> {
        return viewModel.database
                .child(viewModel.gameId!!)
                .setValue(null)
    }
}