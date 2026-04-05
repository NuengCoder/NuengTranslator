package com.nueng.translator.ui.online

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nueng.translator.ui.online.bot.NuengChatBotScreen
import com.nueng.translator.ui.online.friend.AddFriendScreen
import com.nueng.translator.ui.online.friend.AddGroupScreen
import com.nueng.translator.ui.online.friend.FriendChatScreen
import com.nueng.translator.ui.online.friend.FriendInfoScreen
import com.nueng.translator.ui.online.friend.OnlineFriendScreen
import com.nueng.translator.ui.online.global.OnlineGlobalScreen
import com.nueng.translator.ui.online.group.GroupChatScreen
import com.nueng.translator.ui.online.group.GroupInfoScreen
import com.nueng.translator.ui.online.group.AddFriendToGroupScreen
import com.nueng.translator.ui.qr.QrCodeScreen
import com.nueng.translator.ui.qr.ScanQrScreen
import com.nueng.translator.ui.online.group.GroupManageScreen
import com.nueng.translator.ui.online.home.OnlineHomeScreen
import com.nueng.translator.ui.online.profile.OnlineProfileScreen
import com.nueng.translator.ui.online.profile.OtherProfileScreen
import com.nueng.translator.ui.online.settings.OnlineSettingsScreen
import com.nueng.translator.ui.online.forward.ForwardScreen
import com.nueng.translator.ui.online.storage.FileStorageScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun OnlineNavGraph(
    onlineNavController: NavHostController,
    onNavigateBackToHome: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    NavHost(navController = onlineNavController, startDestination = "online_home") {

        composable("online_home") {
            OnlineScaffold(navController = onlineNavController) { mod ->
                OnlineHomeScreen(modifier = mod, onNavigateBack = onNavigateBackToHome)
            }
        }

        composable("online_global") {
            OnlineScaffold(navController = onlineNavController) { mod ->
                OnlineGlobalScreen(
                    modifier            = mod,
                    onNavigateToProfile = { userId ->
                        onlineNavController.navigate("online_other_profile/$userId")
                    },
                    onNavigateToForward = { text ->
                        val encoded = URLEncoder.encode(text, "UTF-8")
                        onlineNavController.navigate("online_forward/$encoded")
                    }
                )
            }
        }

        composable("online_friend") {
            OnlineScaffold(navController = onlineNavController) { mod ->
                OnlineFriendScreen(
                    modifier               = mod,
                    onNavigateToAddFriend  = { onlineNavController.navigate("online_add_friend") },
                    onNavigateToAddGroup   = { onlineNavController.navigate("online_add_group") },
                    onSwitchToProfile      = {
                        onlineNavController.navigate("online_profile") {
                            popUpTo("online_home") { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                    onNavigateToBot        = { onlineNavController.navigate("online_bot") },
                    onNavigateToSelfChat   = { chatId, selfUserId ->
                        onlineNavController.navigate("online_friend_chat/$chatId/$selfUserId")
                    },
                    onNavigateToFriendChat = { chatId, friendUserId ->
                        onlineNavController.navigate("online_friend_chat/$chatId/$friendUserId")
                    },
                    onNavigateToGroupChat  = { groupId ->
                        onlineNavController.navigate("online_group_chat/$groupId")
                    }
                )
            }
        }

        composable("online_profile") {
            OnlineScaffold(navController = onlineNavController) { mod ->
                OnlineProfileScreen(modifier = mod)
            }
        }

        composable("online_settings") {
            OnlineScaffold(navController = onlineNavController) { mod ->
                OnlineSettingsScreen(
                    modifier          = mod,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToDirectory = { dirId, dirName ->
                        val encoded = URLEncoder.encode(dirName, "UTF-8")
                        onlineNavController.navigate("online_directory_words/$dirId/$encoded")
                    },
                    onNavigateToQr = { username ->
                        val encoded = URLEncoder.encode(username, "UTF-8")
                        onlineNavController.navigate("online_qr_code/$encoded")
                    }
                )
            }
        }

        composable(
            route     = "online_qr_code/{username}",
            arguments = listOf(navArgument("username") { type = NavType.StringType })
        ) { back ->
            val username = URLDecoder.decode(
                back.arguments?.getString("username") ?: "", "UTF-8")
            QrCodeScreen(username = username, onNavigateBack = { onlineNavController.popBackStack() })
        }

        composable(
            route     = "online_directory_words/{directoryId}/{directoryName}",
            arguments = listOf(
                navArgument("directoryId")   { type = NavType.LongType },
                navArgument("directoryName") { type = NavType.StringType }
            )
        ) { back ->
            val dirId   = back.arguments?.getLong("directoryId") ?: 0L
            val dirName = URLDecoder.decode(
                back.arguments?.getString("directoryName") ?: "", "UTF-8")
            com.nueng.translator.ui.mynote.DirectoryWordsScreen(
                directoryId   = dirId,
                directoryName = dirName,
                onNavigateBack = { onlineNavController.popBackStack() }
            )
        }

        composable("online_add_friend") {
            AddFriendScreen(
                onNavigateBack      = { onlineNavController.popBackStack() },
                onNavigateToProfile = { userId ->
                    onlineNavController.navigate("online_other_profile/$userId")
                },
                onNavigateToScanQr  = { username ->
                    val encoded = URLEncoder.encode(username, "UTF-8")
                    onlineNavController.navigate("online_scan_qr/$encoded")
                }
            )
        }

        composable(
            route     = "online_scan_qr/{myUsername}",
            arguments = listOf(navArgument("myUsername") { type = NavType.StringType })
        ) { back ->
            val myUsername = URLDecoder.decode(
                back.arguments?.getString("myUsername") ?: "", "UTF-8")
            ScanQrScreen(
                myUsername       = myUsername,
                onNavigateBack   = { onlineNavController.popBackStack() },
                onNavigateToMyQr = { username ->
                    val encoded = URLEncoder.encode(username, "UTF-8")
                    onlineNavController.navigate("online_qr_code/$encoded")
                },
                onQrScanned = { scannedUsername ->
                    onlineNavController.navigate("online_other_profile/$scannedUsername") {
                        popUpTo("online_scan_qr/$myUsername") { inclusive = true }
                    }
                }
            )
        }

        composable("online_add_group") {
            AddGroupScreen(
                onNavigateBack        = { onlineNavController.popBackStack() },
                onNavigateToGroupChat = { groupId ->
                    onlineNavController.popBackStack()
                    onlineNavController.navigate("online_group_chat/$groupId")
                }
            )
        }

        composable(
            route     = "online_other_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: ""
            OtherProfileScreen(
                targetUserId     = userId,
                onNavigateBack   = { onlineNavController.popBackStack() },
                onNavigateToChat = { friendUserId ->
                    onlineNavController.navigate("online_friend_chat/resolve/$friendUserId")
                }
            )
        }

        composable(
            route     = "online_friend_chat/{chatId}/{friendUserId}",
            arguments = listOf(
                navArgument("chatId")       { type = NavType.StringType },
                navArgument("friendUserId") { type = NavType.StringType }
            )
        ) { back ->
            val chatId       = back.arguments?.getString("chatId") ?: ""
            val friendUserId = back.arguments?.getString("friendUserId") ?: ""
            FriendChatScreen(
                chatId                 = chatId,
                friendUserId           = friendUserId,
                onNavigateBack         = { onlineNavController.popBackStack() },
                onNavigateToFriendInfo = { fId ->
                    onlineNavController.navigate("online_friend_info/$fId")
                },
                onNavigateToForward    = { text ->
                    val encoded = URLEncoder.encode(text, "UTF-8")
                    onlineNavController.navigate("online_forward/$encoded")
                },
                onNavigateToGroupChat  = { groupId ->
                    onlineNavController.navigate("online_group_chat/$groupId")
                }
            )
        }

        composable(
            route     = "online_friend_info/{friendUserId}",
            arguments = listOf(navArgument("friendUserId") { type = NavType.StringType })
        ) { back ->
            val friendUserId = back.arguments?.getString("friendUserId") ?: ""
            FriendInfoScreen(
                friendUserId               = friendUserId,
                onNavigateBack             = { onlineNavController.popBackStack() },
                onNavigateBackToFriendList = {
                    onlineNavController.popBackStack("online_friend", inclusive = false)
                },
                onNavigateToFileStorage    = { fUserId, ownerName ->
                    val encoded = URLEncoder.encode(ownerName, StandardCharsets.UTF_8.toString())
                    onlineNavController.navigate("online_file_storage/friend/$fUserId/$encoded")
                }
            )
        }

        composable(
            route     = "online_group_chat/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { back ->
            val groupId = back.arguments?.getString("groupId") ?: ""
            GroupChatScreen(
                groupId               = groupId,
                onNavigateBack        = { onlineNavController.popBackStack() },
                onNavigateToGroupInfo = { gId ->
                    onlineNavController.navigate("online_group_info/$gId")
                },
                onNavigateToForward   = { text ->
                    val encoded = URLEncoder.encode(text, "UTF-8")
                    onlineNavController.navigate("online_forward/$encoded")
                }
            )
        }

        composable(
            route     = "online_add_friend_to_group/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { back ->
            val groupId = back.arguments?.getString("groupId") ?: ""
            AddFriendToGroupScreen(
                groupId        = groupId,
                onNavigateBack = { onlineNavController.popBackStack() }
            )
        }

        composable(
            route     = "online_group_manage/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { back ->
            val groupId = back.arguments?.getString("groupId") ?: ""
            GroupManageScreen(
                groupId             = groupId,
                onNavigateBack      = { onlineNavController.popBackStack() },
                onNavigateToFileStorage = { gId, gName ->
                    val encoded = URLEncoder.encode(gName, StandardCharsets.UTF_8.toString())
                    onlineNavController.navigate("online_file_storage/group/$gId/$encoded")
                }
            )
        }

        composable(
            route     = "online_group_info/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { back ->
            val groupId = back.arguments?.getString("groupId") ?: ""
            GroupInfoScreen(
                groupId                    = groupId,
                onNavigateBack             = { onlineNavController.popBackStack() },
                onNavigateBackToFriendList = {
                    onlineNavController.popBackStack("online_friend", inclusive = false)
                },
                onNavigateToMemberProfile  = { userId ->
                    onlineNavController.navigate("online_other_profile/$userId")
                },
                onNavigateToGroupManage    = { gId ->
                    onlineNavController.navigate("online_group_manage/$gId")
                },
                onNavigateToAddFriendToGroup = { gId ->
                    onlineNavController.navigate("online_add_friend_to_group/$gId")
                },
                onNavigateToFileStorage    = { gId, groupName ->
                    val encoded = URLEncoder.encode(groupName, StandardCharsets.UTF_8.toString())
                    onlineNavController.navigate("online_file_storage/group/$gId/$encoded")
                }
            )
        }

        // ── File Storage — shows all shared .ntf files from a chat ────────
        composable(
            route     = "online_file_storage/{chatType}/{chatId}/{ownerName}",
            arguments = listOf(
                navArgument("chatType")  { type = NavType.StringType },
                navArgument("chatId")    { type = NavType.StringType },
                navArgument("ownerName") { type = NavType.StringType }
            )
        ) { back ->
            val chatType  = back.arguments?.getString("chatType") ?: "friend"
            val chatId    = back.arguments?.getString("chatId") ?: ""
            val ownerName = URLDecoder.decode(
                back.arguments?.getString("ownerName") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            FileStorageScreen(
                chatType       = chatType,
                chatId         = chatId,
                ownerName      = ownerName,
                onNavigateBack = { onlineNavController.popBackStack() }
            )
        }

        composable(
            route     = "online_forward/{messageText}",
            arguments = listOf(navArgument("messageText") { type = NavType.StringType; defaultValue = "" })
        ) { back ->
            val msgText = URLDecoder.decode(
                back.arguments?.getString("messageText") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            ForwardScreen(
                messageText    = msgText,
                onNavigateBack = { onlineNavController.popBackStack() }
            )
        }

        composable("online_bot") {
            NuengChatBotScreen(onNavigateBack = { onlineNavController.popBackStack() })
        }
    }
}
