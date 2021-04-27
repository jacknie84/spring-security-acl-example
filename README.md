# spring-security-acl-example

Spring Security ACL 예제 프로젝트 :+1: :tada:

## 예제 Application 구조

```
+-----------+ 1         n +-------------------+
| Community | ----------> | Community Message |
+-----------+             +-------------------+
      ^                             ^
      |                             |
      |                             |
+-----------------+ 1         n +--------------------------+
| Community Owner | ----------> | Community Message Writer |
+-----------------+             +--------------------------+
```

* 통합관리자(`ROLE_ADMIN`)는 `Community`를 생성할 수 있으며 사용자(`ROLE_USER`)에게 `Community Owner` 권한을 할당 할 수 있습니다.
* `Community Owner`는 `Community` 의 `관리`, `읽기`, `쓰기`, `삭제`, `메시지작성` 권한을 가지게 됩니다. 
* `Community Owner`는 소유하고 있는 `Community`에 사용자(`ROLE_USER`)를 초대 할 수 있습니다.
* 임의의 `Community`에 초대 된 사용자(`ROLE_USER`)는 해당 `Community`의 `읽기`, `메시지작성` 권한과 모든 `Community Message`의 `읽기` 권한을 가지게 됩니다.
* 임의의 `Community`에 초대 된 사용자(`ROLE_USER`)가 해당 `Community`에 `Community Message` 를 작성하게 되면 본 사용자는 `Community Message Writer` 가 됩니다.
* `Community Message Writer`는 작성한 `Community Message`의 `읽기`, `쓰기`, `삭제` 권한을 가질 수 있습니다.
