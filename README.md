# Android Open Lens OCR Text Scanner

## Project Description
Android Open Lens OCR Text Scanner is an open-source Android application that allows users to scan text from images using Optical Character Recognition (OCR). The app leverages modern Android development practices and open-source libraries to provide a fast, accurate, and user-friendly text scanning experience.

## Features
- Capture images using the device camera or select from the gallery
- Extract text from images using OCR technology
- Copy, share, or save extracted text
- Simple and intuitive user interface
- Support for multiple languages (if applicable)
- History of scanned texts

## Getting Started

### Prerequisites
- Android Studio (latest stable version recommended)
- Android device or emulator running Android 6.0 (Marshmallow) or higher
- Java 8+ or Kotlin support

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/Android-Open-Lens-OCR-Text-Scanner.git
   cd Android-Open-Lens-OCR-Text-Scanner
   ```

2. **Open the project in Android Studio:**
   - Select `Open an existing project` and choose this folder.

3. **Install dependencies:**
   - Android Studio will automatically sync and download dependencies via Gradle.

4. **Run the app:**
   - Connect your Android device or start an emulator.
   - Click the 'Run' button in Android Studio.

## Project Overview for New Contributors

This project follows a modular Android architecture for maintainability and scalability.

### Major Folders & Files

- **app/**: Main Android application module containing source code.
  - **src/main/java/**: Contains all Java/Kotlin source files.
    - **activities/**: UI screens and logic.
    - **fragments/**: Reusable UI components.
    - **ocr/**: OCR processing logic and helpers.
    - **utils/**: Utility classes and helpers.
  - **src/main/res/**: App resources (layouts, drawables, strings, etc.).
  - **src/main/AndroidManifest.xml**: App manifest file.
- **screenshots/**: Contains screenshots for documentation and PRs.
- **build.gradle**: Project and module build configuration files.
- **README.md**: Project documentation.

### Architecture

- **Frontend/UI**: Built with Android native components (Activities, Fragments, XML layouts).
- **OCR Engine**: Integrates open-source OCR libraries (e.g., Tesseract or ML Kit) for text extraction.
- **Data Storage**: Uses local storage (e.g., Room database or SharedPreferences) for saving scan history.
- **No separate backend**: All processing is done on-device for privacy and speed.

### Component Interaction

- The UI layer (Activities/Fragments) interacts with the OCR logic to process images.
- Extracted text is displayed to the user and can be saved or shared.
- Data storage components manage scan history and user preferences.

## Getting Started for Developers

1. **Fork and clone the repository.**
2. **Open in Android Studio and let Gradle sync dependencies.**
3. **Familiarize yourself with the folder structure and main components.**
4. **Run the app on an emulator or device to see it in action.**
5. **Check the issues tab for open tasks or feature requests.**

## Roadmap

- [ ] Add support for more OCR languages
- [ ] Improve UI/UX with Material Design
- [ ] Add cloud backup for scan history
- [ ] Implement dark mode
- [ ] Add unit and UI tests

## Contributing

We welcome contributions from everyone! Please read the guidelines below before submitting a pull request.

## Contributing Guidelines

- Every pull request (PR) **must include relevant app screenshots** showing the changes made.
- Add these screenshots to the `screenshots/` folder in the repository.
- Update the **Screenshots** section in the README to include the new screenshots with appropriate captions or context.
- Ensure screenshots are clearly labeled (e.g., `feature-login.png`, `fix-navbar-bug.png`) and correspond to the PR functionality.
- Follow standard Android development best practices and ensure your code passes lint and builds successfully.

## Screenshots

<table>
  <tr>
    <td align="center">
      <b>Main Screen</b><br>
      <img src="https://github.com/user-attachments/assets/cee758a7-07fd-4d70-94d9-28318eb4821b" width="200px" />
    </td>
    <td align="center">
      <b>Text Selection Overlay</b><br>
      <img src="https://github.com/user-attachments/assets/99737c09-502f-4b31-8747-7ec751d135c4" width="200px" />
    </td>
    <td align="center">
      <b>Extracted Text View</b><br>
      <img src="https://github.com/user-attachments/assets/b48ad992-6a53-4e72-9041-c20d57aa95cd" width="200px" />
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>Margin Rulers for Columns</b><br>
      <img src="https://github.com/user-attachments/assets/7ab6b590-a041-4816-9fd9-d13b094abfed" width="200px" />
    </td>
    <td align="center">
      <b>Open Image from Browser</b><br>
      <img src="https://github.com/user-attachments/assets/8b780832-d477-4449-a535-9cf5133bd267" width="200px" />
    </td>
    <td align="center">
      <b>Zoom Feature</b><br>
      <img src="https://github.com/user-attachments/assets/60ff92dc-35f1-4541-969a-ba4e7b545af4" width="200px" />
    </td>
  </tr>
</table>

<!-- Add new screenshots in the table above with a short description above each image -->

Open sourced alternative for Google Lens


Grab & extract text from an image using smart text selection cursors overlaid on the image.

OCR App recognizes text in any Latin-based language.

To recognize the text in an image, OCR App uses Google firebase ML Kit's On-device text recognition APIs
https://firebase.google.com/docs/ml-kit/recognize-text

## Options:
1. Zoom the image.
2. Alter the extracted text before copying if needed.
3. Also works on a 2 column text layout using the left & right margin rulers.
4. Open an image from Internet browser directly without downloading to device.

## Contribution
 * GNU AFFERO GENERAL PUBLIC LICENSE
 * Copyright and license notices must be preserved.
 * When a modified version is used to provide a service over a network, the complete source code of the modified version must be made available.
