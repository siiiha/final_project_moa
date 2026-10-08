# Git 사용 가이드

파이널 프로젝트 전 Git/GitHub 협업 흐름을 연습하기 위한 가이드입니다.

작업은 항상 `develop` 브랜치를 기준으로 새로운 브랜치를 생성하여 진행합니다.

---

## 1. Repository Clone

최초 1회 실행합니다.

```bash
git clone Repository주소
```

아래 명령어를 통해 프로젝트 폴더로 이동하거나 git bash창을 끈 후에 프로젝트 내부에서 git bash창을 다시 띄웁니다.

```bash
cd Repository
```

---

## 2. develop 브랜치 준비

원격의 `develop` 브랜치를 로컬에 생성합니다.

```bash
git checkout -b develop origin/develop
```

브랜치 확인

```bash
git branch
```

---

## 3. 작업 시작

작업 전 반드시 `develop` 브랜치로 이동합니다.

```bash
git checkout develop
```

최신 내용을 가져옵니다.

```bash
git pull origin develop
```

작업용 브랜치를 생성합니다.

### 브랜치 생성 규칙

```bash
git checkout -b feature/(도메인-)기능
```

예시

```bash
git checkout -b feature/member-login
git checkout -b feature/member-login-exception
git checkout -b feature/unlogin-contect
```

---

## 4. 작업 후 Commit

변경사항 확인

```bash
git status
```

변경 파일 Stage에 추가

```bash
git add .
```

파일 단위로 Stage에 추가

```bash
git add 파일명
```

Commit

```bash
git commit -m "feat: 로그인 기능 구현"
git commit -m "fix: 비로그인 접근 시 interceptor 경로 추가"
git commit -m "docs: README.md 수정"
```

---

## 5. 최신 원격의 develop 상태 반영

Push 전에 최신 `develop` 내용을 가져옵니다.

```bash
git pull origin develop
```

충돌이 발생하면 충돌을 해결한 후 Stage에 올린 후에 다시 Commit합니다.

```bash
git add .
git commit -m "merge: develop과 충돌 해결"
```

---

## 6. Push

최초 Push

```bash
git push -u origin 브랜치명
```

예시

```bash
git push -u origin feature/member-login
```

한번이라도 Push한적 있는 브랜치 수정 후 Push

```bash
git push origin 브랜치명
```

예시

```bash
git push origin docs/edit-README
```

---

## 7. Pull Request 생성

GitHub에서

```text
[Pull requests] 메뉴 클릭
        ↓
우측 상단에 [New pull request] 녹색 버튼 클릭
        ↓
작업한 브랜치 [feature/기능명] 클릭
        ↓
왼쪽 상단에 merge하는 base브랜치를 develop으로 변경
        ↓
[Create pull request] 녹색 버튼 클릭
        ↓
충돌나는 부분 없는지 확인 / 충돌 난다면 local에서 해결 후 push까지 다시 진행
```

GitHub에서 Pull Request를 생성합니다.
<br>
```text
작업 Branch
      ↓
   develop
```

반드시 `base` 브랜치가 `develop`인지 확인합니다.

### PR 제목

```text
[Docs] REST API 학습 내용 작성
[Feat] 로그인 기능 구현
[Fix] 비로그인 접근 시 Interceptor 경로 추가
```

### PR 내용

```markdown
## 작업 내용
- 게시글 등록
- 게시글 수정
- 게시글 삭제

## 작업 파일
- BoardController.java
- BoardService.java
- BoardMapper.xml

## 테스트 방법
1. 정상 동작 확인
2. 예외 상황 확인 (체크 리스트 작성)
3. 기존 기능 영향 확인

## 스크린샷
<!-- 코드/화면 선택 첨부 -->

## 참고사항
- 부가적으로 설명할 내용
- 이슈사항
- 팀원들한테 질문해도됨
```

---

## 8. Code Review

다른 팀원의 PR을 확인하고 의견을 남깁니다.

단순히 `확인했습니다.`보다는

```text
이 부분은 제가 공부한 내용과 조금 다른 것 같습니다.

PUT과 PATCH의 차이에 대한 예시도 추가하면
이해하기 더 쉬울 것 같습니다.
```

```text
BoardMapper.xml에서의 SQL쿼리문이 정렬이 안되어있는것 같습니다.
order by절을 추가해주시면 좋을 것 같습니다.
```

처럼 학습 내용을 서로 공유하거나 검토를 해주시면 좋을 것 같습니다.

Review 후 수정할 내용이 있다면 기존 브랜치에서 수정 후 다시 Commit / Push합니다.

---

## 9. Merge(병합) 후 정리

PR이 `develop`에 Merge되면 로컬도 최신화합니다.

```bash
git checkout develop
```

```bash
git pull origin develop
```
<br>
사용이 끝난 로컬 브랜치를 삭제합니다.

```bash
git branch -d 브랜치명
```

예시

```bash
git branch -d feature/member-login
```

---

## Commit Message 규칙

형식

```text
type: 작업 내용
```

| Type       | 설명         |
| ---------- | ---------- |
| `feat`     | 기능 추가      |
| `fix`      | 오류 수정      |
| `docs`     | 문서 작성 및 수정 |
| `refactor` | 코드 구조 개선   |
| `test`     | 테스트 코드     |

예시

```text
docs: REST API 개념 정리
docs: Exception Handling 학습 내용 수정
feat: 로그인 기능 구현
fix: 로그인 시 아이디 정규식 검사 로직 추가
refactor: 중복되는 유효성 검사 로직 하나의 메소드로 분리
```

---

## 기본 규칙

- `main`, `develop`에서 직접 작업하지 않습니다.
- 작업 시작 전 `develop`을 최신화합니다.
- 하나의 Branch에서는 하나의 작업만 진행합니다. (ex. type/동사-명사)
- Commit Message만 보고 작업 내용을 알 수 있도록 작성합니다.
- 작업 내용은 반드시 Pull Request와 Review를 거쳐 `develop`에 Merge합니다.
- Conflict가 발생하면 임의로 삭제하지 말고 변경 내용을 확인한 뒤 해결합니다.

---

## 자주 사용하는 Git 명령어

현재 로컬의 브랜치 확인

```bash
git branch
```
<br>


변경사항 확인

```bash
git status
```
<br>


변경사항 임시저장

```bash
git stash
```
<br>


임시저장 목록 확인

```bash
git stash list
```

마지막 임시저장 현재 브랜치로 불러오기 및 목록에서 삭제

```bash
git stash pop
```

마지막 임시저장 현재 브랜치로 불러오기 및 목록에서 삭제

```bash
git stash apply stash@{숫자}
```
<br>


브랜치 이동

```bash
git checkout develop
```
<br>


새로운 브랜치 생성

```bash
git checkout -b 브랜치명
```
<br>


다른 브랜치의 상태 가져오기

```bash
git pull 브랜치명
```

원격의 브랜치를 가져올 때에는 브랜치명 앞에 origin추가

```bash
git pull origin 브랜치명
```
<br>


변경사항 Stage 추가

```bash
git add .
```
<br>


Commit

```bash
git commit -m "커밋메시지"
```
<br>


최초 Push

```bash
git push -u origin 브랜치명
```



이후 Push

```bash
git push origin 브랜치명
```
<br>


로컬 브랜치 삭제

```bash
git branch -d 브랜치명
```
