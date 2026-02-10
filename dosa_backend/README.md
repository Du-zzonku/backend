# 🛠️ 이공계열 학생을 위한 3D 뷰어 기반 학습 솔루션 (Backend)

> **블레이버스 해커톤 시즌 4 출품작 - 팀 두쫀쿠** > **복잡한 기계 구조를 웹에서 분해/조립하며 학습하는 인터랙티브 3D 솔루션**

<br>

## 📖 Project Overview
**"복잡한 기계 구조, 책으로만 배우기엔 한계가 있지 않을까요?"**

본 프로젝트는 이공계열 학생들을 타겟으로 한 **3D 기반 학습 보조 도구**입니다.  
단순한 이미지나 텍스트를 넘어, 웹상에서 직접 부품을 분해하고 조립하며 기계의 내부 구조를 직관적으로 이해할 수 있도록 돕습니다.  
**3D Viewer**를 중심으로 **AI Agent, 퀴즈, 메모, PDF 저장** 기능을 통합하여 학습 효율을 극대화했습니다.

<br>

## 👥 Team Info (두쫀쿠)

| Role | Name |       Position       | Github / Contact |
|:---:|:---:|:--------------------:|:---:|
| **PM** | 신경철 |   Project Manager    | |
| **PD** | 김유빈 |   Product Designer   | |
| **FE** | 김규민 |  Frontend Developer  | |
| **BE** | 김민구 |  Backend Developer   | |
| **BE** | 구동한 |  Backend Developer   | |
| **BE** | 탁찬홍 | Full Stack Developer | |

<br>

## 🛠️ Tech Stack

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.1
- **Database**: PostgreSQL
- **Build Tool**: Gradle
- **Docs**: Swagger (Springdoc OpenAPI)

### 3D Pipeline & Others
- **3D Tools**: Blender (Python Scripting)
- **Visualization**: Three.js
- **Infra**: AWS EC2, AWS RDS

<br>

## 🚀 Key Features & Implementation
### 1. Interactive 3D Viewer (탁찬홍)
* **기능**: 웹상에서 기계 부품을 자유롭게 회전, 확대, 분해(Explode), 조립(Assemble) 가능.
* **구현**: GLB 파일 로딩 및 Three.js 기반 렌더링 최적화.

### 2. Quiz System (탁찬홍)
* **기능**: 학습한 모델(Model)과 연관된 부품(Part)에 대한 퀴즈 풀이 및 자동 채점.
* **구현**: `QuizService`를 통해 랜덤 문제 출제 및 해설 제공 API 구축.

### 3. PDF Export (To be updated)
* **담당**: 김민구
* **내용**: (작성 예정 - 예: iText 라이브러리를 활용하여 학습 메모와 퀴즈 결과를 PDF로 변환 및 다운로드 기능 구현)

### 4. AI Agent (To be updated)
* **담당**: 구동한
* **내용**: (작성 예정 - 예: OpenAI API를 연동하여 사용자의 질문에 대해 기계 구조 정보를 바탕으로 답변하는 튜터 봇 구현)

<br>

## ERD
<img width="710" height="567" alt="스크린샷 2026-02-10 오후 2 40 09" src="https://github.com/user-attachments/assets/20c00b53-c55d-4f1d-991e-c18983fd009e" />

## Sequence Diagram

<img width="8191" height="3869" alt="GLB Model Placement Flow-2026-02-06-051222" src="https://github.com/user-attachments/assets/bbae0ad4-4891-404a-9607-08677b2c4be2" />


## ⚡ Technical Challenges & Solutions

프로젝트 진행 중 마주친 기술적 난관과 해결 과정입니다.

### 1. 3D 모델 좌표 데이터 정합성 문제 (Data Pipeline)
* **Problem**: 제공받은 GLB(3D 모델) 파일에는 단순 부품 정보만 포함되어 있고, 웹상에서 조립/분해 애니메이션을 구현하기 위한 **상대적 좌표값(Position/Rotation)** 데이터가 부재했습니다.
* **Solution**:
    1. **Blender** 툴을 활용하여 모든 부품을 직접 가상 조립했습니다.
    2. Blender 내장 **Python Scripting**을 이용하여 조립된 각 부품의 정확한 좌표값과 회전값을 추출했습니다.
    3. 추출된 데이터를 Three.js에서 즉시 사용할 수 있는 JSON 포맷으로 변환하여 프론트엔드로 전달하는 파이프라인을 구축했습니다.
* **Result**: 부품의 조립(`assemble`) 및 해체(`explode`) 모션을 위한 정확한 벡터값 확보 성공.

### 2. 3D 렌더링 속도 최적화 (Optimization)
* **Problem**: 고해상도 HDR(조명/배경) 파일(1.2MB) 사용 시, Fast 4G 네트워크 환경 기준 **초기 로딩에 약 7초**가 소요되어 사용자 경험(UX)이 저하되었습니다.
* **Solution**:
    1. HDR 이미지의 해상도를 256x128 픽셀로 리사이징 및 압축을 진행했습니다.
    2. 파일 크기를 **128KB**로 약 90% 절감했습니다.
* **Result**: 전체 렌더링 로딩 시간을 7초에서 **4초**로 약 **43% 단축**했습니다.

### 3. 렌더링 퍼포먼스 개선 (Rendering Performance)
* **Problem**: 다수의 부품을 동시에 조립/해체하는 애니메이션 실행 시, React의 상태 관리(`useState`)로 인한 잦은 리렌더링으로 **프레임 드랍(잔렉)**이 발생했습니다.
* **Solution**:
    1. 애니메이션 관련 로직에서 React의 `useState`를 배제하고 **`useRef`**를 도입했습니다.
    2. React의 가상 돔(Virtual DOM) 리렌더링 프로세스를 거치지 않고, Three.js의 캔버스 객체를 직접 조작(Direct Manipulation)하도록 변경했습니다.
* **Result**: 끊김 없는 부드러운 60fps 조립/해체 애니메이션 구현.

<br>

## 📂 API Documentation
서버 실행 후 아래 주소에서 API 명세를 확인할 수 있습니다.
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html` (로컬 기준)

<br>

## ⚙️ How to Run
```bash
# 1. Clone the repository
git clone https://github.com/Du-zzonku/backend

# 2. Configure Database (src/main/resources/application.yml)
# spring.datasource.url=jdbc:postgresql://localhost:5432/your_db
# spring.datasource.username=your_username
# spring.datasource.password=your_password

# 3. Build
./gradlew clean build

# 4. Run
./gradlew bootRun
