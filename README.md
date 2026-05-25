# WebIDE - AI-Collaborated Web for Android IDE

## 📖 Project Introduction

Built with Jetpack Compose. The biggest feature of this project is that it was **entirely developed by AI**, showcasing the immense potential of AI in software development.

## 🤖 AI Development

This project is the result of collaboration between multiple AI models:

- **Claude**: Responsible for writing the welcome screen and theme system
- **Gemini**: Developed the main UI and file tree components  
- **DeepSeek**: Co-developed some core features of the code editor alongside Gemini

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Target Platform**: Android

## 📁 Project Structure

```text
app/src/main/java/com/web/webide/
├── core/           # Core business logic
├── files/          # File management module
├── html/           # HTML processing
├── textmate/       # Syntax highlighting support
├── ui/             # User Interface layer
│   ├── components/ # Reusable components
│   ├── editor/     # Code editor
│   ├── preview/    # Live preview
│   ├── projects/   # Project management
│   ├── settings/   # Settings screen
│   ├── theme/      # Theme system
│   └── welcome/    # Welcome screen
├── App.kt          # Application entry point
└── MainActivity.kt # Main activity
