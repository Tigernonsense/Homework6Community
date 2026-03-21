# Homework 6 Community

Welcome to the **Homework 6 Community** — recreational programming competitions inspired by RPI's CSCI 1200 Data Structures contest.

💡 Build the fastest solver  
🧩 Create challenging puzzles  
🏆 Compete against others and win prizes 

## 🧩 Puzzles

- Pips (https://www.nytimes.com/games/pips) (ONGOING)

## 📜 Rules

- Don't hardcode the answers (obviously).
- You can use any programming language you want.
- Using obscure or weird programming languages can get you special bonuses.
- You may make your own puzzles and add them to the contest, but everyone must agree on what the answer is.

---

## Join our Discord

👉 https://discord.gg/VKjMK8QW

## Using the Leaderboard

### Build:
```bash
./gradlew shadowJar
```

### Run (with memory tracking):
```bash
java --enable-native-access=ALL-UNNAMED -jar leaderboard/build/libs/leaderboard-all.jar contests/.../leaderboard.cfg --timeout=10
```

### Run (without memory tracking):
```bash
java -jar leaderboard/build/libs/leaderboard-all.jar contests/.../leaderboard.cfg --disable-memory-tracker --timeout=10
```
