package com.nueng.translator.ui.online.bot

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class BotMessage(
    val text: String,
    val isUser: Boolean
)

@HiltViewModel
class NuengChatBotViewModel @Inject constructor() : ViewModel() {

    private val _messages = MutableStateFlow(
        listOf(BotMessage("Hello! I am NuengChatBot. How can I help you today?", isUser = false))
    )
    val messages: StateFlow<List<BotMessage>> = _messages.asStateFlow()

    fun sendMessage(text: String) {
        val userMsg = BotMessage(text, isUser = true)
        val botReply = BotMessage(getReply(text), isUser = false)
        _messages.value = _messages.value + userMsg + botReply
    }

    private fun getReply(input: String): String {
        val q = input.lowercase().trim()
        return when {
            // Identity
            contains(q, "who are you", "what are you", "your name") ->
                "I am NuengChatBot, the built-in assistant for NuengTranslator! I can answer questions about the app and help you get started."

            contains(q, "hello", "hi", "hey", "greet", "howdy") ->
                "Hello there! Great to see you. What would you like to know about NuengTranslator?"

            contains(q, "how are you", "how r u") ->
                "I am always running at 100%! How can I help you today?"

            contains(q, "thank", "thanks", "thx") ->
                "You are welcome! Feel free to ask anything else."

            contains(q, "bye", "goodbye", "see you", "cya") ->
                "Goodbye! Come back anytime if you need help. Happy studying!"

            // App overview
            contains(q, "what is nuengtranslator", "what does this app do", "about this app", "app do") ->
                "NuengTranslator is a language learning and translation app supporting English, Thai, Chinese, Lao, Vietnamese, and Indonesian. You can translate words, study HSK and IELTS vocabulary, save personal notes, and chat with other users online!"

            // Languages
            contains(q, "language", "support", "which lang") ->
                "NuengTranslator supports 6 languages: English, Thai (ไทย), Chinese (中文), Lao (ລາວ), Vietnamese (Tiếng Việt), and Indonesian (Bahasa Indonesia)."

            // Translate
            contains(q, "translat", "how to search", "search word") ->
                "Go to the Translate tab at the bottom. Select your source and target language, then type or use voice input, camera OCR, or the draw feature to search for words!"

            // My Note
            contains(q, "my note", "mynote", "save word", "save note") ->
                "In the My Note tab, you can create directories to organize your saved words. Tap + to add a directory, then tap into it to add your own vocabulary with translation and example sentences."

            // Study
            contains(q, "study", "hsk", "ielts", "flashcard", "learn") ->
                "The Study tab has HSK 1-6 (Chinese vocabulary) and IELTS 1-6 (English vocabulary) packs. Each pack has a flashcard mode and a list mode. Tap a card to reveal the translation, swipe to go to the next word!"

            // Pinyin
            contains(q, "pinyin", "tone", "pronunciation") ->
                "In the Study tab, the Pinyin tab teaches you all Chinese vowels and consonants with their 4 tones. Tap any tone to hear it spoken out loud!"

            // Stroke
            contains(q, "stroke", "write chinese", "draw") ->
                "In the Study tab, the Strokes tab teaches you Chinese stroke order rules. In the Translate tab, you can also draw a character using the draw icon to search for it!"

            // Chat / Online
            contains(q, "online", "chat", "nuengchat", "friend") ->
                "NuengChat is the online social feature! You can chat globally, add friends, create group chats, and set up your online profile with a nickname and bio."

            // Global chat
            contains(q, "global", "public chat", "global chat") ->
                "The Global Chat is a public world chat where everyone can see messages. It is wiped clean every day at 7 AM to keep things fresh!"

            // Profile
            contains(q, "profile", "nickname", "avatar", "bio") ->
                "In the OProfile tab, you can see your profile. In OSettings, you can set your nickname (changeable every 14 days), write a bio, and your avatar is auto-generated from the first letter of your nickname."

            // Rank
            contains(q, "rank", "devadmin", "vip", "premium", "normal rank") ->
                "There are different ranks: Normal (default), VIP, Premium, and the special DevAdmin rank for the app developer. VIP and Premium ranks will be available via the payment feature in a future update!"

            // Settings
            contains(q, "setting", "dark mode", "theme", "language setting") ->
                "In the Settings tab (main app), you can toggle Dark Mode, change the UI language, delete your note data, or logout. In OSettings, you manage your online profile."

            // Admin
            contains(q, "admin", "admin panel") ->
                "The Admin Panel is accessible only to the NuengAdmin account. It lets the admin manage users, add words to the shared language dictionary, and monitor the word database."

            // OCR
            contains(q, "camera", "ocr", "scan text", "photo") ->
                "In the Translate tab, tap the camera icon to open Camera OCR. Point your camera at any text and tap the capture button. The app will extract the text and let you search for it instantly!"

            // Voice
            contains(q, "voice", "microphone", "speak", "mic") ->
                "In the Translate tab, tap the microphone icon to use voice input. Speak the word you want to search and it will appear in the search bar automatically!"

            // TTS
            contains(q, "listen", "pronounce", "tts", "text to speech", "speak out") ->
                "In the Study Pinyin tab, each vowel and consonant has a speaker icon. Tap the normal icon for regular speed or the slow-motion icon for slower pronunciation to help you learn!"

            // Block
            contains(q, "block", "unblock") ->
                "You can block another user from their profile screen. Blocked users appear in your OSettings Block List and they will not be able to message you. You can unblock them anytime from there."

            // Friend request
            contains(q, "friend request", "add friend", "how to add") ->
                "To add a friend, go to the OFriend tab and tap the person+ icon. Enter the user's ID to find their profile, then tap Add Friend to send a request!"

            // Group
            contains(q, "group", "group chat", "create group") ->
                "To create a group, tap the group+ icon in the OFriend tab. Select friends from your list and create the group. You will be the group admin and can manage members!"

            // Update / version
            contains(q, "version", "update", "latest", "v1") ->
                "NuengTranslator is currently on version 1.5! New features are being added regularly including group chats, global chat improvements, and more social features. Stay tuned!"

            // Developer
            contains(q, "developer", "who made", "creator", "made by", "nueng") ->
                "NuengTranslator was created by Nueng (NuengAdmin). The DevAdmin rank is reserved for the developer. The app is under active development!"

            // Joke
            contains(q, "joke", "funny", "haha", "lol") ->
                "Why do programmers prefer dark mode? Because light attracts bugs! Ha, just like how dark mode is default in NuengTranslator!"

            // Default
            else ->
                "I am not sure about that one. You can ask me about app features like translation, My Note, Study packs, NuengChat, profiles, or anything else about NuengTranslator!"
        }
    }

    private fun contains(input: String, vararg keywords: String): Boolean {
        return keywords.any { input.contains(it) }
    }
}
