# Nami

This is the last public version of nami made for minecraft 1.21.11
It wont receive any updates

### Join our discord - https://discord.gg/auHTtNAqRq

<p>
  <a href="https://github.com/NamiDevelopment/nami/releases">
    <img src="https://img.shields.io/github/downloads/NamiDevelopment/nami/total?color=green&label=Total%20Downloads" alt="Total Downloads" />
  </a>
  <a href="https://github.com/NamiDevelopment/nami/commits">
  <img src="https://img.shields.io/github/commit-activity/m/NamiDevelopment/nami?label=Commits%20(last%20month)&color=yellow" alt="month" />
  </a>
  <a href="https://github.com/NamiDevelopment/nami/releases">
    <img src="https://img.shields.io/github/v/release/Kiriyaga7615/nami?color=blue&label=Latest%20Release" alt="Latest Release" />
  </a>
  <a href="https://discord.gg/auHTtNAqRq">
    <img src="https://img.shields.io/discord/1298742596633497744?color=7289DB&label=Discord" alt="Discord" />
  </a>
</p>

![# badge](assets/readme/no-stops-no-regrets.svg)
![# badge](assets/readme/ensuring-code-integrity.svg)
![# badge](assets/readme/works-on-selfmerging.svg)


**Nami** is a modular and lightweight anarchy client base built for PVE and automation.  

Most popular Minecraft clients are closed-source, paid, and obfuscated, making them difficult to audit or trust. Some may include backdoors or malicious code.

This project started as a clean, open-source alternative aiming to be transparent, secure, and easy to extend without relying on unsafe third-party clients.

---

## Screenshots

<details>
<summary>View screenshots</summary>

<img width="1920" height="1080" alt="ClickGUI" src="assets/clickgui.png" />
<img width="1920" height="1080" alt="HudEditor" src="assets/hudeditor.png" />
<img width="1920" height="1080" alt="Friends" src="assets/friends.png" />
<img width="1920" height="1080" alt="Friends" src="assets/configs.png" />

</details>

---

## Plugin development

See https://github.com/NamiDevelopment/template-plugin for information

---

## FAQ

<details>
<summary>How to open ClickGUI?</summary>

Default keybind is: P  

</details>

<details>
<summary>What is the command prefix?</summary>

The default command prefix is `-`.

</details>

---

## Requirements

- Java 21  
- Gradle 8+  
- Minecraft 1.21.11 
- Fabric loader, API

---

## How to Build

1. Clone the repository:

    ```bash
    git clone https://github.com/NamiDevelopment/Nami.git  
    cd nami
    ```
2. In order to get nami-api dependency,you need to configure your PAT-token in your root .gradle/gradle.dependency

3. Build with Gradle:

    ```bash
    ./gradlew build
    ```

The compiled JAR will be located at:  
`build/libs/nami-<version>.jar`

nami-client is packaged with nami-api inside of it.

---

## License

This project is licensed under the MIT License. You are free to contribute, distribute, fork, or reuse any part.

---

## Special Thanks

- [cattyngmd](https://github.com/cattyngmd)

- [CatFormat](https://github.com/cattyngmd/CatFormat)
