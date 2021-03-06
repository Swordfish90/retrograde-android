/*
 * GameLoader.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.swordfish.lemuroid.lib.game

import com.gojuno.koptional.None
import com.swordfish.lemuroid.lib.core.CoreManager
import com.swordfish.lemuroid.lib.library.GameLibrary
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.gojuno.koptional.Optional
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.functions.Function3
import io.reactivex.schedulers.Schedulers
import java.io.File

class GameLoader(
    private val coreManager: CoreManager,
    private val retrogradeDatabase: RetrogradeDatabase,
    private val gameLibrary: GameLibrary
) {

    fun loadGame(gameId: Int): Maybe<Game> = retrogradeDatabase.gameDao().selectById(gameId)

    fun load(gameId: Int, loadSave: Boolean): Single<GameData> {
        return loadGame(gameId)
                .subscribeOn(Schedulers.io())
                .flatMapSingle { game -> prepareGame(game, loadSave) }
    }

    private fun prepareGame(game: Game, loadSave: Boolean): Single<GameData> {
        val gameSystem = GameSystem.findById(game.systemId)!!

        val coreObservable = coreManager.downloadCore(gameSystem.coreFileName)
        val gameObservable = gameLibrary.getGameRom(game)
        val saveObservable = if (loadSave) {
            gameLibrary.getGameSave(game)
        } else {
            Single.just(None)
        }

        return Single.zip(
                coreObservable,
                gameObservable,
                saveObservable,
                Function3<File, File, Optional<ByteArray>, GameData> { coreFile, gameFile, saveData ->
                    GameData(game, coreFile, gameFile, saveData.toNullable())
                })
    }

    @Suppress("ArrayInDataClass")
    data class GameData(
        val game: Game,
        val coreFile: File,
        val gameFile: File,
        val saveData: ByteArray?
    )
}
