# HANDOFF

이 문서는 claude.ai 채팅 세션에서 이 프로젝트를 작성한 뒤, Claude Code 세션으로 작업을 넘기기 위한 인수인계 문서다. 새 세션은 이 파일을 먼저 읽고 시작할 것.

---

## 0. 이 프로젝트의 현재 상태 (가장 중요)

**빌드 통과. 명세 전 항목 실서버 검증 완료.**

`gradlew build` 성공, 산출물 `build/libs/courierpost-1.0.0.jar`. 26.2 전용 서버에 직접 만든 프로토콜 프로브로 접속시켜 **명세 항목을 전부 실측 검증했다** (6.1 ~ 6.5). 글로벌/귓속말/팀 채팅 배달, 거리와 속도, 날씨 배율, 차원 이동 양쪽 분기, 발전과제 개인별 현지화, 재시작 복원, 엔티티 규칙, 확률·수명 분기, 100인/1000마리 부하까지 포함한다. 남은 것은 7장.

초기 버전은 Yarn 매핑으로 작성돼 있었으나, **MC 26.x부터 Fabric은 Yarn을 쓰지 않는다.** Yarn 빌드는 1.21.11에서 멈췄고 26.2는 Mojang 공식 매핑을 그대로 쓴다 (Loom이 리매핑을 하지 않으므로 `mappings` 선언도 `modImplementation`도 없다). 전체 소스를 공식 매핑으로 포팅했다.

포팅 근거는 추측이 아니라 Loom이 뽑아낸 실제 jar
(`~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/`)를 `javap`로 열어 확인한 시그니처다. 이후 API 의문이 생기면 같은 방법을 쓸 것. 기억이나 위키보다 정확하다.

26.2에서 바뀐 것 중 눈에 띄는 것:

- `ResourceLocation` -> `net.minecraft.resources.Identifier` (Mojang이 이름을 바꿨다)
- 권한 체계 전면 개편. `hasPermissionLevel(int)`이 사라지고 `net.minecraft.server.permissions` 패키지가 생겼다. 명령 요구 권한은 `Commands.hasPermission(Commands.LEVEL_OWNERS)`, 플레이어 개별 판정은 `Commands.LEVEL_GAMEMASTERS.check(player.permissions())`.
- 게임룰이 `net.minecraft.world.level.gamerules`로 이동하고 `GameRule<T>` 타입이 됐다. `announceAdvancements`의 새 이름은 `GameRules.SHOW_ADVANCEMENT_MESSAGES`, 조회는 `getGameRules().get(rule)`.
- `com.mojang.authlib.GameProfile`이 record다. `getName()`이 아니라 `name()`.
- `Entity.getServer()`가 없다. `player.level().getServer()`를 쓴다.
- 커맨드 태그 조회는 `Entity.entityTags()`, 좌표/각도 설정은 `Entity.snapTo(...)`.
- 닭 관련 클래스가 `net.minecraft.world.entity.animal.chicken` 패키지로 내려갔고, 엔티티 타입 상수는 `EntityType`이 아니라 `EntityTypes`에 있다.
- `ClientboundUpdateAdvancementsPacket` 생성자 인자가 5개다 (`shouldShowAdvancements` 추가).

빌드에 **JDK 25**가 필요하다. Gradle 툴체인 자동 탐지 경로인 `~/.gradle/jdks/jdk-25.0.4+7`에 Temurin 25를 넣어뒀으므로 별도 설정 없이 `gradlew build`가 동작한다. 이 디렉터리를 지우면 JDK 25를 다시 마련해야 한다.

---

## 1. 프로젝트 개요

Minecraft Java 26.2 / Fabric / **서버 사이드 전용** 모드.

모든 채팅이 즉시 전달되지 않고, 전서구(닭 엔티티)가 수신자에게 물리적으로 날아가서 도착했을 때 배달된다. 거리가 곧 지연 시간이 된다.

원본 명세서는 사용자가 별도로 보유하고 있다. 명세와 충돌하는 판단이 필요하면 임의로 정하지 말고 사용자에게 물을 것.

### 절대 제약

- **클라이언트 설치를 요구하지 않는다.** `environment: "server"` 유지.
- **리소스팩을 요구하지 않는다.**
- **소스 코드에 주석을 일절 쓰지 않는다.** 라인 주석, 블록 주석, javadoc, TODO, 생성 표식 전부 금지. 설명은 소스 밖에서만. 이 문서와 대화가 설명의 자리다.
- 이모지, 이모티콘, 장식용 유니코드 금지.
- 플레이스홀더/목 구현 금지. 프로덕션 코드만.

### 네이밍 제약 (중요)

"Roost"는 실존 앱 이름이다. 명세서에 비제휴 disclaimer가 있지만, **코드 식별자·패키지·리소스 경로·저장 파일명·태그값 어디에도 roost를 넣지 않는다.** 사용자의 명시적 요청으로 전면 개명을 이미 수행했다.

현재: 패키지 `com.courierpost`, mod id `courierpost`, 진입점 `CourierPost`, 태그 `courierpost_bird`, 설정 `config/courierpost.json`, 저장 `<world>/data/courierpost_birds.json`.

`fabric.mod.json`의 표시 이름(`"name": "The Courier's Roost"`)과 로거 라벨만 남아 있다. 이건 사람이 읽는 문자열이라 의도적으로 남긴 것이다. 사용자가 지우라고 하면 지운다.

---

## 2. 구조

```
com.courierpost
├── CourierPost                    진입점. 설정 로드, 이벤트 등록, 매니저 수명주기
├── config/CourierConfig           14개 설정 항목. JSON 저장/로드, 키 기반 get/set
├── bird/
│   ├── CourierBird                전서구 1마리의 상태 (순수 데이터)
│   ├── BirdManager                핵심. 틱 시뮬레이션, 추적, 배달, 엔티티 동기화
│   ├── BirdStorage                직렬화 (재시작 복원, 30초 자동 저장)
│   ├── BirdVariants               아기 닭 변종. 버전 민감 API 격리용
│   └── MessageKind                GLOBAL / WHISPER / TEAM
├── chat/
│   ├── ChatHandler                발송 진입점. 쿨다운, 발전과제 지급, 공지
│   ├── CourierText                메시지 텍스트 구성
│   └── Localization               서버 측 언어 해석
├── command/CourierCommands        /birdconfig, /birds, /birdtrack (권한 4)
├── advancement/AdvancementLocalizer  발전과제 플레이어별 현지화
└── mixin/
    ├── MsgCommandMixin                     /msg /w /tell 가로채기
    ├── TeamMsgCommandMixin                 /teammsg /tm 가로채기
    ├── ChickenMixin                        전서구의 알 낳기 억제
    ├── MobMixin                            몹이 전서구를 타겟하지 못하게
    └── ServerCommonPacketListenerImplMixin 발전과제 패킷 재작성
```

글로벌 채팅은 믹스인이 아니라 Fabric API `ServerMessageEvents.ALLOW_CHAT_MESSAGE`로 취소한다.

---

## 3. 설계 결정과 그 이유

새 세션이 "왜 이렇게 했지" 하고 되돌리지 않도록 근거를 남긴다.

### 3.1 언어 처리가 이원화된 이유

서버 전용이라 클라이언트에 우리 lang 파일이 없다. 그래서 두 갈래로 나뉜다.

- **모드 고유 문구** — `Localization`이 jar 내부 `/assets/courierpost/lang/*.json`을 읽고, `player.clientInformation().language()`로 플레이어 언어를 판별해 서버에서 직접 치환한 뒤 **리터럴**로 전송한다. `ko*` → 한국어, `ja*` → 일본어, 그 외 영어.
- **바닐라 키** — `chat.type.text`, `commands.message.display.incoming`, `chat.type.team.text`, `chat.type.advancement.task` 등은 클라이언트가 이미 갖고 있으므로 `Component.translatable`을 그대로 쓴다. 26.2 `en_us.json`에 다섯 키 모두 실존하는 것을 확인했다.

**후자를 리터럴로 바꾸지 말 것.** 클라이언트가 이미 가진 키를 서버가 문자열로 박아 보내면, 폰트/포맷 커스터마이징이나 접근성 리소스팩을 쓰는 플레이어가 손해를 본다.

`assets/lang`을 남겨둔 이유는 나중에 클라이언트 배포판이 필요해지면 `environment`만 `"*"`로 바꿔서 그대로 쓸 수 있게 하기 위해서다.

### 3.2 발전과제 현지화 (여기가 제일 공들인 부분)

문제: 발전과제 정의는 전역이라 플레이어별 언어 적용이 불가능해 보인다. 리소스팩 배포는 "리소스팩 불요" 제약 위반이고, 클라이언트 배포는 "클라 설치 불요" 제약과 충돌한다.

해법: 발전과제 정의는 전역이지만 **클라이언트로 나가는 `ClientboundUpdateAdvancementsPacket`은 플레이어마다 따로 직렬화된다.** 그 시점에 우리 발전과제의 display 텍스트만 해당 플레이어 언어의 리터럴로 갈아끼우면 토스트와 발전과제 화면이 개인별 언어로 나온다. 서버 전용 유지, 클라 무설치, 리소스팩 불요 전부 만족.

- `ServerCommonPacketListenerImplMixin`이 `send` 인자를 `@ModifyVariable`로 교체한다. HEAD에서 취소 후 재전송하면 무한 재귀라 이 방식을 썼다. 되돌리지 말 것.
- 우리 발전과제가 없으면 원본 패킷을 그대로 반환하므로 다른 발전과제에는 비용이 0이다.
- `DisplayInfo.setLocation`으로 좌표를 복사해 발전과제 화면 배치를 유지한다.

**채팅 공지는 별도 처리다.** 바닐라는 공지 텍스트를 한 번만 만들어 전원에게 뿌리므로 패킷 재작성만으로는 공지가 현지화되지 않는다. 그래서 `announce_to_chat`을 `false`로 내리고 `ChatHandler.grantAdvancement`에서 직접 브로드캐스트한다. `chat.type.advancement.task`(클라이언트가 가진 키)에 시청자 언어로 만든 대괄호 제목을 넣어 바닐라 형식을 그대로 재현했고, `GameRules.SHOW_ADVANCEMENT_MESSAGES` 게임룰도 확인한다. 중복 지급 방지로 `getOrStartProgress().isDone()` 선검사가 있다.

`announce_to_chat`을 다시 `true`로 올리면 공지가 두 번 나간다.

### 3.3 사망/분실 확률

생성 시 1회 판정하고, 예상 비행 시간 내 임의 시점에 발동하도록 예약한다. 매 틱 굴리면 거리에 비례해 확률이 폭증해서 `bird_death_chance: 0.002`가 의미를 잃는다.

### 3.4 배달 메시지는 시스템 메시지

배달 시점에는 서명 체인이 이미 만료됐다. `PlayerChatMessage`로 재전송할 수 없어 `sendSystemMessage`로 보낸다.

### 3.5 기타

- `show_bird`를 런타임에 껐다 켜도 비행 중인 개체에 즉시 반영된다 (`syncEntity`에서 처리).
- 서버 시작 시 태그 붙은 고아 닭을 첫 틱에 일괄 정리한다.
- 차원 이동은 엔티티를 폐기하고 새 월드에 재생성한다. 버전마다 자주 바뀌는 teleport API를 피하기 위해서다.
- 영속화는 NBT 대신 Gson JSON을 쓴다. 마찬가지로 API 변동 회피 목적이고, 1000마리 규모에서 성능 문제 없다.

---

## 3.6 멀티버전 구조

26.1.2 / 26.2 / 26.3을 하나의 소스 트리로 지원한다. 공통 코드는 `src/main/java`에 두고, 버전마다 갈리는 부분만 `src/compat/` 아래 레이어로 분리한 뒤 `build.gradle`이 대상 버전에 맞는 디렉터리를 `sourceSets.main`에 붙인다.

```
src/compat/advancements/legacy   26.1.2, 26.2   DisplayInfo 게터 + setLocation, 패킷 getAdded/getRemoved/...
src/compat/advancements/record   26.3           DisplayInfo 레코드 접근자, PositionedAdvancement
src/compat/entity/v26_1          26.1.2         EntityType.CHICKEN,  setInvulnerable
src/compat/entity/v26_2          26.2           EntityTypes.CHICKEN, setInvulnerable
src/compat/entity/v26_3          26.3           EntityTypes.CHICKEN, setPermanentlyInvulnerable
```

버전을 추가할 때는 `build.gradle`의 `targets`(Fabric API 버전)와 `compatLayers`(레이어 조합) 두 맵에 항목을 넣는다.

26.x 사이에서 실제로 갈린 지점은 이 셋뿐이다.

1. `EntityType` -> `EntityTypes` (26.2에서 개명)
2. `Entity.setInvulnerable` -> `setPermanentlyInvulnerable` (26.3)
3. `DisplayInfo`와 `ClientboundUpdateAdvancementsPacket`이 26.3에서 레코드가 되면서 게터 이름이 바뀌고, 발전과제 화면 좌표가 `DisplayInfo`에서 빠져 패킷의 `PositionedAdvancement`로 옮겨갔다

실행 디렉터리도 버전별로 갈라 뒀다 (`run/26.1.2`, `run/26.2`, `run/26.3`). 같은 월드를 여러 버전으로 열면 월드가 상위 버전으로 업그레이드돼 되돌릴 수 없다.

---

## 4. 빌드

```
cd courierpost
./gradlew collect -Pmc=26.2
```

대상 버전은 `-Pmc`로 고른다 (`26.1.2`, `26.2`, `26.3`). 생략하면 `default_minecraft_version`. 산출물은 `dist/courierpost-1.0.0+<버전>.jar`.

버전별 Fabric API는 `build.gradle`의 `targets` 맵에 박혀 있고, 전부 실측치다.

```
26.1.2 -> 0.155.3+26.1.2
26.2   -> 0.161.0+26.2
26.3   -> 0.161.0+26.3
loader_version=0.19.5
loom_version=1.17.19
```

확인처: `https://fabricmc.net/develop/`, `https://meta.fabricmc.net/v2/versions/loader/26.2`,
그리고 `https://github.com/FabricMC/fabric-example-mod` 의 `master` 브랜치 빌드 파일. 마지막 것이 가장 확실하다 — 최신 MC에 맞춰 갱신되고, 플러그인 ID나 의존성 선언 방식이 바뀌면 거기서 먼저 드러난다.

Gradle 9.5.1 wrapper 포함. Gradle 데몬은 JDK 21에서도 돌지만 컴파일은 툴체인 25로 fork된다.

---

## 5. 믹스인 타깃 (26.2 기준 실측)

바닐라 시그니처가 바뀌면 조용히 적용 실패하는 지점이라 정확한 형태를 남긴다.

1. `MsgCommand.sendMessage(CommandSourceStack, Collection<ServerPlayer>, PlayerChatMessage)` — private static void
2. `TeamMsgCommand.sendMessage(CommandSourceStack, Entity, PlayerTeam, List<ServerPlayer>, PlayerChatMessage)` — private static void.
   팀 미소속 오류는 바닐라가 이 호출 전에 처리하므로 믹스인에서 다시 볼 필요가 없다. 대상 목록과 팀 객체를 인자로 그대로 받는다.
3. `ServerCommonPacketListenerImpl.send(Lnet/minecraft/network/protocol/Packet;)V` — 2인자 오버로드가 따로 있으므로 디스크립터를 반드시 명시할 것
4. `Chicken.aiStep()` / `@Shadow public int eggTime`
5. `Mob.canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z`

---

## 6. 이미 통과한 검증

이건 다시 안 해도 된다. 단, 코드를 고친 뒤에는 다시 볼 것.

- `gradlew build` 통과 (컴파일 + 믹스인 AP + jar)
- JSON 6개 전부 유효
- `fabric.mod.json` entrypoint 클래스 실존, mixins.json 파일명 참조 일치
- 믹스인 선언 5개 == 실제 파일 5개, 전체 `package` 선언 == 디렉터리 경로
- 언어 파일 3종 키 집합 완전 일치, `%s` 개수 13개 키 전부 언어별 동일
- 코드 참조 키 13개 == lang 정의 키 (미사용/미정의 0)
- `Localization.text` 호출 14건 인자 개수 불일치 0
- 설정 KEYS == `get()` == `set()` == `writeJson()` == 명세서 14개 항목
- 직렬화 save 필드 == load 필드, transient 3개만 제외
- 발전과제 criteria `sent` == 코드의 `award` 인자, ID 네임스페이스 일치

### 6.1 서버 기동 스모크 테스트

`gradlew runServer`로 26.2 전용 서버를 실제로 띄워 확인했다 (`run/`에 평지 월드가 남아 있어 재실행이 빠르다).

- 믹스인 5개 적용 중 오류 없음, 크래시 없음
- `onInitializeServer` 실행됨 (`Restored 0 courier birds` 로그, `run/config/courierpost.json` 자동 생성, 항목 14개 전부 명세 기본값과 일치)
- 콘솔에서 `birdconfig get bird_speed` -> `bird_speed = 25.0`
- `birdconfig set bird_speed 30` -> `bird_speed set to 30.0` (즉시 반영)
- `birds` -> `No active courier birds.`
- `birdtrack` -> `A player is required to run this command here` (콘솔은 플레이어가 아니므로 정상)
- `stop`으로 정상 종료

주의: 콘솔 명령을 `Done (...)` 로그가 뜨기 **전에** 밀어넣으면 바닐라 `stop`을 포함해 전부 `An unexpected error occurred`로 실패한다. 모드 문제가 아니라 기동 타이밍 문제다. 스크립트로 명령을 넣을 거면 `Done (`을 기다린 뒤에 보낼 것.

### 6.2 프로토콜 프로브를 이용한 실접속 검증

Minecraft 클라이언트 없이, 원시 소켓으로 26.2 프로토콜을 직접 말하는 프로브를 만들어 실제 플레이어로 접속시켜 검증했다. 상태 핑으로 프로토콜 버전(776)을 얻고, 로그인 -> 설정(configuration) -> 플레이 단계까지 진행한 뒤 패킷 페이로드에서 UTF-8 문자열을 찾는 방식이다.

검증된 것:

- `/msg` 가로채기 (`MsgCommandMixin`)와 발신자 피드백
- **발전과제 패킷 재작성.** 클라이언트 언어를 `ko_kr`로 보고한 접속에서, 서버가 보낸 `ClientboundUpdateAdvancementsPacket`(play 0x82) 페이로드에 `비둘기야 먹자~` / `구구구구구구! 마시쪙`가 들어 있었다. 3.2의 설계가 실제로 동작한다는 뜻이다. 채팅 공지(system chat)에도 같은 한국어 제목이 실렸다.
- **전서구 실배달 (2인 접속).** `Sender`가 `Receiver`에게 귓속말을 보내자 `Receiver`가 **8291 ms 후** 메시지를 수신했다. `base_dimension_delay: 8`(=8000 ms)에 근거리 비행 시간이 더해진 값으로, 지연이 설정대로 적용되고 배달이 실제로 일어난다는 것을 보여준다.
- **글로벌 채팅.** 서명 채팅 패킷(play 0x09)을 보내 `ServerMessageEvents.ALLOW_CHAT_MESSAGE` 경로를 탔다. 발신자는 즉시 자기 메시지와 `Courier bird dispatched.`를 받았고, 수신자는 **8322 ms 후** 배달받았다.
- **글로벌 채팅 쿨다운.** 10초 안에 두 번째 발언을 시도하자 `You must wait N more seconds before speaking again`이 돌아왔다.
- 전 과정에서 서버 로그에 예외 없음.

### 6.3 2차 프로브 검증

같은 프로브를 확장해 나머지 기능을 확인했다.

- **팀 채팅.** `Sender`와 `Receiver`를 같은 팀에 넣고 `/teammsg`를 보내자 **8330 ms 후** 팀원이 수신했다. `TeamMsgCommandMixin`이 실제로 발동한다.
- **일본어.** 클라이언트 언어 `ja_jp`로 접속하니 `伝書鳩を送りました。`가 나왔다. 이로써 ko/en/ja 3종 모두 확인.
- **비행 중 `/birds`.** 전서구가 떠 있는 동안 조회하니 `飛行中の伝書鳩:` 목록이 나왔다. 살아 있는 새를 대상으로 한 명령 출력 경로가 동작한다.
- **속도와 거리.** `bird_speed`를 0.5로 낮추자 배달이 8.3초에서 **22197 ms**로 늘었다. 거리 나누기 속도 계산이 실제로 적용된다.
- **날씨 배율.** 같은 조건에서 비 **26904 ms**, 뇌우 **36212 ms**. 기본 지연 8초를 뺀 순수 비행 시간으로 보면 맑음 14197 / 비 18904 / 뇌우 28212 ms이고, 비율이 각각 1.331배(1/0.75 = 1.333)와 1.987배(1/0.5 = 2.0)다. `rain_multiplier`와 `thunder_multiplier`가 정확히 그대로 반영된다.
- **런타임 설정 변경.** 위 측정은 전부 `/birdconfig set`으로 값을 바꿔가며 했다. 재시작 없이 즉시 반영된다.
- **재시작 후 복원.** `base_dimension_delay`를 300으로 올려 비행 중인 새를 남긴 채 서버를 정지하니 `world/data/courierpost_birds.json`에 필드 19개가 온전히 직렬화됐고, 재기동 시 `Restored 1 courier birds`가 찍혔다.

### 6.4 3차 프로브 검증

- **차원 이동, `use_coordinate_conversion: false`.** 플레이어를 옮길 수 없어서(아래 프로브 제약 참조), 저장 파일에 네더 좌표(x=100)를 가진 전서구를 직접 써 넣고 복원시켰다. 수신자가 오버월드에 접속하자 전서구가 오버월드 (0,0)으로 진입해 **즉시** 배달했다.
- **차원 이동, `use_coordinate_conversion: true`.** 같은 새를 같은 방식으로 넣고 설정만 바꾸니 **32379 ms** 걸렸다. 네더 x=100 × 8.0 = 오버월드 x=800이고 800블록 ÷ 25 = 32초이므로 바닐라 차원 배율이 그대로 적용된 것이다. 비행 도중 자동 저장된 파일에 오버월드 x=345가 찍혀 실시간 추적도 함께 확인됐다.
- **엔티티 규칙.** 비행 중인 전서구의 NBT를 통째로 덤프했다. `Tags:["courierpost_bird"]`, `NoAI:1b`, `Invulnerable:1b`, `NoGravity:1b`, `Silent:1b`, `PersistenceRequired:1b`, `Motion:[0d,0d,0d]`, 타입 `minecraft:chicken`, `EggLayTime:2147483646`. 마지막 값이 `ChickenMixin`이 매 틱 `Integer.MAX_VALUE`를 박아 넣고 있다는 증거다.
- **아기 닭 비율과 변종.** 전서구 100마리를 띄우고 `sort=random`으로 `Age`를 80회 표본 추출하니 음수(아기)가 10건이었다 (12.5%, 명세 10%). 변종 표본에서 기본값 `temperate` 외에 `cold`가 관측돼 변종 지정도 동작한다.
  주의: 아기 닭 판정에 `nbt={IsBaby:1b}`를 쓰면 안 된다. 닭은 `Age`로 아기 상태를 저장하고, 그 값은 매 틱 증가하므로 `nbt={Age:-24000}` 같은 정확한 일치도 생성 직후 한 틱만 유효하다.
- **확률·수명 분기.** `bird_death_chance: 1.0` -> 발신자에게 사망 메시지, 수신자는 못 받음. `lost_chance: 1.0` -> 수신자도 못 받고 발신자에게 아무 메시지도 안 감(분실은 조용히 처리). `bird_max_lifetime: 1` -> 지연 60초짜리 전서구가 4초 뒤 목록에서 사라짐.
- **`deliver_offline_messages`.** `false`에서 수신자가 접속을 끊자 전서구가 즉시 폐기됐다(`No active courier birds.`). `true`에서는 수신자가 나가도 전서구가 유지됐다.

### 6.5 부하 측정 (100인 / 1000마리)

봇 100대를 실제로 접속시키고 글로벌 채팅 10회로 정확히 1000마리를 만든 뒤 `/debug start` `/debug stop`으로 TPS를 쟀다.

| 조건 | TPS |
| --- | --- |
| 플레이어 100명, 전서구 0마리 | 19.56 |
| 플레이어 100명, 전서구 1000마리, `show_bird: true` | **4.45** |
| 플레이어 100명, 전서구 1000마리, `show_bird: false` | **20.01** |

**모드의 시뮬레이션 자체는 비용이 없다.** 1000마리를 틱하면서도 20.01 TPS가 나온다. TPS를 무너뜨리는 것은 전적으로 스폰된 닭 1000마리를 근처 100명에게 브로드캐스트하는 바닐라 엔티티 트래킹 비용이다.

다만 이 측정은 최악의 조건이다. 봇 100명이 전부 스폰 지점에 겹쳐 있어 1000마리가 100명 전원의 시야에 동시에 들어갔고, 프로브 JVM(스레드 101개)이 같은 머신에서 CPU를 나눠 썼다. 실제 서버라면 플레이어가 흩어져 있어 훨씬 낫다.

운영상의 결론: 대규모 서버에서 TPS가 문제되면 `show_bird`를 끄면 된다. 배달 로직은 그대로 돌고 엔티티만 사라지며, 런타임에 바꿔도 즉시 반영된다(1000마리가 떠 있는 상태에서 껐더니 엔티티는 0이 되고 `/birds`는 계속 1000을 보고했다).

프로브 소스는 프로젝트에 포함하지 않았다. 재현하려면 다시 만들어야 한다. 필요한 정보:

- 패킷 ID는 `javap -p -c`로 `GameProtocols` / `ConfigurationProtocols`의 `<clinit>` 등록 순서를 읽어서 얻는다. 순서가 곧 ID다. 버전이 바뀌면 반드시 다시 뽑을 것.
- 프레이밍을 단순하게 하려고 `run/server.properties`에서 `network-compression-threshold=-1`, `pause-when-empty-seconds=0`으로 두었다. 압축을 켜면 프레임 앞에 `dataLength` VarInt가 추가돼 프로브를 고쳐야 한다.
- 26.2 기준 유용한 ID: 설정 clientbound 3=finish, 5=ping, 14=select_known_packs / 설정 serverbound 0=client_information, 3=finish, 5=pong, 7=select_known_packs / 플레이 clientbound 0x31=login, 0x48=player_position, 0x79=system_chat, 0x82=update_advancements / 플레이 serverbound 0x00=accept_teleportation, 0x07=chat_command, 0x09=chat, 0x1C=keep_alive, 0x2C=player_loaded
- 프로브로는 **플레이어를 텔레포트할 수 없다.** `/tp`는 성공 메시지를 돌려주지만 위치가 원래대로 남고, 클라이언트로 위치 패킷도 오지 않는다. 로그인 시점의 텔레포트는 확인 패킷을 보내면 정상 처리되는데 그 뒤 `/tp`는 안 먹는다. 원인은 못 찾았고, 진짜 클라이언트가 보내는 이동 패킷이 없어서로 추정된다. 그래서 거리 효과는 플레이어를 옮기는 대신 `bird_speed`를 낮춰 측정했다.

## 7. 검증하지 못한 것

명세 항목은 전부 확인했다. 남은 것은 성격상 자동 검증이 어려운 것들이다.

- **눈으로 본 적이 없다.** 좌표와 NBT로는 상승 -> 강하가 맞게 돌아가지만, 실제 클라이언트 화면에서 전서구가 자연스럽게 보이는지(회전 각도, 강하 궤적, 아기 닭 크기)는 사람이 봐야 안다.
- 진짜 클라이언트 접속. 모든 검증은 직접 만든 프로토콜 프로브로 했다. 바닐라 클라이언트가 우리가 보내는 패킷을 전부 정상으로 받아들이는지는 확인하지 못했다. 다만 우리는 바닐라가 만든 패킷을 그대로 통과시키거나(발전과제만 텍스트 교체) 시스템 메시지를 쓸 뿐이라 위험은 낮다.
- 장시간 안정성. 가장 긴 세션이 7분이다. 메모리 누수나 장시간 누적 문제는 모른다.
- 여러 차원에 플레이어가 흩어진 상태에서의 동시 배달. 차원 이동은 단일 전서구로만 확인했다.
