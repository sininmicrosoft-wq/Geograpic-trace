package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RedeemedReward
import com.example.data.model.RewardQuest
import com.example.data.model.UserRewardProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {

    @Query("SELECT * FROM user_reward_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserRewardProfile?>

    @Query("SELECT * FROM user_reward_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): UserRewardProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserRewardProfile)

    @Query("SELECT * FROM reward_quests ORDER BY isClaimed ASC, isCompleted DESC, rewardCoins ASC")
    fun getAllQuestsFlow(): Flow<List<RewardQuest>>

    @Query("SELECT * FROM reward_quests")
    suspend fun getAllQuestsSync(): List<RewardQuest>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<RewardQuest>)

    @Update
    suspend fun updateQuest(quest: RewardQuest)

    @Query("UPDATE reward_quests SET currentValue = :value, isCompleted = CASE WHEN :value >= targetValue THEN 1 ELSE 0 END WHERE id = :id")
    suspend fun updateQuestProgress(id: String, value: Double)

    @Query("UPDATE reward_quests SET isClaimed = 1 WHERE id = :id")
    suspend fun markQuestClaimed(id: String)

    @Query("SELECT * FROM redeemed_rewards ORDER BY redeemedTimestamp DESC")
    fun getAllRedeemedRewardsFlow(): Flow<List<RedeemedReward>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedeemedReward(reward: RedeemedReward): Long

    @Query("UPDATE user_reward_profile SET totalCoins = totalCoins + :coins, lifetimeCoinsEarned = lifetimeCoinsEarned + :coins WHERE id = 1")
    suspend fun addCoins(coins: Int)

    @Query("UPDATE user_reward_profile SET totalCoins = totalCoins - :coins WHERE id = 1 AND totalCoins >= :coins")
    suspend fun deductCoins(coins: Int): Int
}
