# The Courier's Roost

모든 채팅이 즉시 전달되지 않는다. 전서구(닭)가 수신자에게 실제로 날아가고, 도착했을 때 메시지가 배달된다. 거리가 곧 지연 시간이다.

서버 사이드 전용 모드다. 접속자는 모드를 설치할 필요가 없고, 리소스팩도 필요 없다.

느린 메시징이라는 발상은 Roost 앱에서 따왔다. 이 프로젝트는 비공식이며 Roost와 제휴하거나 승인받은 관계가 아니다.

---

## 요구사항

| 항목 | 버전 |
| --- | --- |
| Minecraft | 26.1.2 / 26.2 / 26.3 전용 서버 |
| Fabric Loader | 0.19.3 이상 |
| Fabric API | 해당 버전 대응판 |
| Java | 25 이상 |

세 버전 모두 Java 25를 요구한다. Java 21로는 서버가 뜨지 않는다. 아래 설명에서 `<버전>`은 셋 중 하나로 바꿔 읽는다.

---

## 서버 설치

### 1. 서버 폴더를 만든다

```
mkdir courier-server
cd courier-server
```

### 2. Fabric 서버 런처를 받는다

```
https://meta.fabricmc.net/v2/versions/loader/<버전>/0.19.5/1.1.2/server/jar
```

받은 파일을 `fabric-server-launch.jar`로 저장한다. 이 런처는 첫 실행 때 바닐라 서버 jar와 라이브러리를 알아서 받는다.

### 3. mods 폴더에 jar 두 개를 넣는다

```
courier-server/
  mods/
    fabric-api-<버전에 맞는 것>.jar
    courierpost-1.0.0+<버전>.jar
```

- Fabric API: https://modrinth.com/mod/fabric-api 에서 서버와 같은 버전용을 받는다
- courierpost: 이 저장소의 `dist/courierpost-1.0.0+<버전>.jar`

버전을 섞으면 Fabric이 로드를 거부한다.

### 4. EULA에 동의한다

첫 실행 후 생성되는 `eula.txt`를 열어 `eula=true`로 바꾼다. Minecraft EULA에 동의한다는 뜻이므로 내용을 확인하고 직접 바꿀 것.

### 5. 실행한다

```
java -Xms2G -Xmx4G -jar fabric-server-launch.jar nogui
```

`java`가 25 미만이면 25 경로를 직접 지정한다.

```
"C:\Program Files\Eclipse Adoptium\jdk-25\bin\java.exe" -Xms2G -Xmx4G -jar fabric-server-launch.jar nogui
```

기동 로그에 다음 줄이 보이면 정상이다.

```
[The Courier's Roost] Restored 0 courier birds
```

### 6. 접속한다

서버와 같은 버전의 Minecraft 클라이언트로 접속한다. 같은 PC면 `localhost`다. 모드 설치는 필요 없다.

---

## 개발용 빠른 실행

소스를 가진 상태라면 서버를 따로 구성할 필요 없이 Loom이 띄워준다.

```
./gradlew runServer -Pmc=26.2
```

실행 디렉터리는 버전별로 갈라져 있다 (`run/26.2` 등). 콘솔에 `stop`을 입력하면 종료된다.

---

## 설정

`config/courierpost.json`에 생성된다. 첫 기동 시 기본값으로 만들어진다.

| 키 | 기본값 | 의미 |
| --- | --- | --- |
| `base_dimension_delay` | 8 | 모든 메시지에 붙는 기본 지연 (초) |
| `bird_speed` | 25.0 | 전서구 속도 (블록/초) |
| `nether_speed` | 16.0 | 네더에 있을 때의 속도 |
| `speed_variation` | 0.2 | 개체별 속도 편차. 0.2면 정해진 속도의 ±20% |
| `rain_multiplier` | 0.75 | 비 올 때 속도 배율 (오버월드만) |
| `thunder_multiplier` | 0.5 | 뇌우일 때 속도 배율 (오버월드만) |
| `bird_death_chance` | 0.002 | 비행 중 사망 확률. 사망하면 메시지는 영구 소실되고 발신자에게 알림이 간다 |
| `lost_chance` | 0.0 | 분실 확률. 아무 알림 없이 조용히 사라진다 |
| `use_coordinate_conversion` | false | 차원이 다를 때 좌표 처리 방식. 아래 설명 참조 |
| `deliver_offline_messages` | false | 수신자가 접속을 끊으면 전서구를 폐기할지 여부 |
| `save_birds` | true | 서버 재시작 시 비행 중인 전서구를 복원할지 여부 |
| `bird_max_lifetime` | 4800 | 이 시간(초)을 넘긴 전서구는 제거된다 |
| `show_bird` | true | 전서구를 실제 엔티티로 스폰할지 여부 |
| `global_chat_cooldown` | 10 | 일반 채팅 재발언 대기 시간 (초). 오퍼레이터는 면제 |

값 변경은 재시작 없이 즉시 반영된다.

```
/birdconfig set bird_speed 30
/birdconfig get bird_speed
/birdconfig reload
```

### use_coordinate_conversion

발신자와 수신자가 다른 차원에 있을 때의 동작이 갈린다.

- `false` — 전서구가 목적지 차원의 `0, 0`에 나타나 거기서부터 수신자에게 날아간다.
- `true` — 바닐라 차원 배율로 좌표를 환산한다. 네더 `100, 0`은 오버월드 `800, 0`이 된다.

---

## 명령어

전부 권한 레벨 4가 필요하다.

| 명령 | 설명 |
| --- | --- |
| `/birdconfig get <키>` | 값 조회 |
| `/birdconfig set <키> <값>` | 값 변경, 즉시 반영 |
| `/birdconfig reload` | 설정 파일 다시 읽기 |
| `/birds` | 비행 중인 전서구 전체 목록 (발신자, 수신자, 차원, 속도, 경과 시간, 남은 거리) |
| `/birdtrack` | 자신이 보낸 전서구만 표시 |

---

## 성능 (대규모 서버 운영자용)

플레이어 100명 / 전서구 1000마리 조건에서 측정한 결과다.

| 조건 | TPS |
| --- | --- |
| 플레이어 100명, 전서구 0마리 | 19.56 |
| 플레이어 100명, 전서구 1000마리, `show_bird: true` | 4.45 |
| 플레이어 100명, 전서구 1000마리, `show_bird: false` | 20.01 |

**모드의 시뮬레이션 자체는 비용이 사실상 없다.** 1000마리를 매 틱 추적하면서도 20.01 TPS가 나온다. TPS를 떨어뜨리는 것은 스폰된 닭 1000마리를 주변 플레이어 전원에게 브로드캐스트하는 바닐라 엔티티 트래킹 비용이다.

따라서 규모가 커져 TPS가 문제되면 전서구 표시를 끈다.

```
/birdconfig set show_bird false
```

배달 로직은 그대로 동작하고 엔티티만 사라진다. 비행 중인 전서구가 있는 상태에서 꺼도 즉시 반영된다.

위 측정은 최악의 조건이었다. 봇 100명이 전부 스폰 지점에 겹쳐 있어 1000마리가 100명 전원의 시야에 동시에 들어갔다. 실제 서버는 플레이어가 흩어져 있으므로 이보다 낫다. 소규모 서버에서는 `show_bird`를 켜 두어도 무방하다.

---

## 지원 언어

한국어, 영어, 일본어. 플레이어의 Minecraft 언어 설정을 따라간다. 같은 서버에서 각자 다른 언어로 표시된다.

---

## 지원 버전

| Minecraft | 산출물 |
| --- | --- |
| 26.1.2 | `courierpost-1.0.0+26.1.2.jar` |
| 26.2 | `courierpost-1.0.0+26.2.jar` |
| 26.3 | `courierpost-1.0.0+26.3.jar` |

각 jar은 해당 버전에서만 로드된다. `fabric.mod.json`에 `~<버전>`으로 고정돼 있으므로 섞어 쓰면 Fabric이 거부한다.

## 빌드

```
./gradlew collect -Pmc=26.2
```

`-Pmc`로 대상 버전을 고른다. 생략하면 `gradle.properties`의 `default_minecraft_version`을 쓴다. 산출물은 `dist/`에 모인다.

세 버전을 모두 만들려면 세 번 돌린다.

```
./gradlew collect -Pmc=26.1.2
./gradlew collect -Pmc=26.2
./gradlew collect -Pmc=26.3
```

JDK 25가 필요하다. 세 버전 모두 Java 25를 요구한다.

개발 관련 상세 내용은 `HANDOFF.md`를 볼 것.

---

## 라이선스

MIT
