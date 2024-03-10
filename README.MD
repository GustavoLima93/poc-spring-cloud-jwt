# Poc autenticação JWT com Spring Cloud Gateway

- Este repositório aborda como implementar autenticação JWT (JSON Web Token) utilizando Spring Cloud Gateway.

- [Site para gerar jwt](https://jwtbuilder.jamiekurtz.com/)

## Sumário

- [Dependências Necessárias](#dependências-necessárias)
- [Classe RouterValidator](#classe-routervalidator)
    - [Lista de Endpoints Abertos](#lista-de-endpoints-abertos)
    - [Predicado para Determinar Segurança](#predicado-para-determinar-segurança)
- [Classe JwtUtil](#classe-jwtutil)
    - [Injeção de Valor de Propriedade](#injeção-de-valor-de-propriedade)
    - [Variável key](#variável-key)
    - [Método de Inicialização](#método-de-inicialização)
    - [Obtenção de Todas as Reivindicações de um Token](#obtenção-de-todas-as-reivindicações-de-um-token)
    - [Verificação de Expiração do Token](#verificação-de-expiração-do-token)
    - [Verificação de Validade do Token](#verificação-de-validade-do-token)
- [Classe AuthenticationFilter](#classe-authenticationfilter)
    - [Implementação da Interface GatewayFilter](#implementação-da-interface-gatewayfilter)
    - [Dependências](#dependências)
    - [Construtor com Injeção de Dependência](#construtor-com-injeção-de-dependência)
    - [Implementação do Método filter](#implementação-do-método-filter)
    - [Métodos Auxiliares](#métodos-auxiliares)
- [Classe GatewayConfig](#classe-gatewayconfig)
    - [Construtor com Injeção de Dependência](#construtor-com-injeção-de-dependência-gatewayconfig)
    - [Bean RouteLocator](#bean-routelocator)
    - [Configuração das Rotas](#configuração-das-rotas)


## [Dependências Necessárias](#dependências-necessárias)

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.1</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.1</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.1</version>
    <scope>runtime</scope>
</dependency>
```
## [Classe RouterValidator](#classe-routervalidator)

- A classe RouterValidator definida é um componente do Spring que provê uma maneira de determinar se uma requisição HTTP deve ser considerada segura e, portanto, requer autenticação, ou se ela pode passar sem autenticação. Vamos detalhar cada parte dessa classe:

### [Lista de Endpoints Abertos](#lista-de-endpoints-abertos)

```java
public static final List<String> openApiEndpoints = List.of(
    "/auth"
);
```

- Aqui, é definida uma lista constante de endpoints que são considerados abertos, ou seja, não exigem autenticação para serem acessados. Neste caso, apenas o endpoint /auth está incluído na lista. Esta lista pode ser expandida para incluir quaisquer outros endpoints que devem ser acessíveis sem autenticação.

### [Predicado para Determinar Segurança](#predicado-para-determinar-segurança)

```java
public Predicate<ServerHttpRequest> isSecured =
        request -> openApiEndpoints
                .stream()
                .noneMatch(uri -> request.getURI().getPath().contains(uri));
```

- Esta método define um **`Predicate<ServerHttpRequest>`** chamado **`isSecured`**. Um predicado é uma função que recebe um argumento e retorna um valor booleano. Neste caso, o predicado é usado para testar requisições HTTP e determinar se elas são "seguras", ou seja, se exigem autenticação.
- A lógica dentro do predicado itera (**`stream()`**) sobre a lista de **`openApiEndpoints`** e usa **`noneMatch`** para verificar se nenhum dos URIs abertos coincide com o caminho da URI da requisição atual. O método **`contains(uri)`** é utilizado para essa comparação.
    - Se **`noneMatch`** retornar **`true`**, significa que o caminho da requisição atual não contém nenhum dos URIs na lista **`openApiEndpoints`**, indicando que a requisição deve ser considerada segura e, portanto, necessita de autenticação.
    - Se **`noneMatch`** retornar **`false`**, significa que a requisição atual corresponde a um dos endpoints abertos e, portanto, não requer autenticação.

## [Classe JwtUtil](#classe-jwtutil)


A classe JwtUtil é um componente no Spring que fornece funcionalidades relacionadas ao tratamento de JSON Web Tokens (JWTs). Vamos examinar cada parte desta classe para entender seu propósito e como ela funciona.

### [Injeção de Valor de Propriedade](#injeção-de-valor-de-propriedade)

```java
@Value("${jwt.secret}")
private String secret;
```
- Utiliza a anotação **`@Value`** para injetar o valor da propriedade **`jwt.secret`** do arquivo de configuração (como **`application.properties`** ou **`application.yml`**) na variável **`secret`**. Este valor é usado como a chave secreta para assinar e verificar os tokens JWT.

### [Variável key](#variável-key)

```java
private Key key;
```
- Declara uma variável **`key`** do tipo **`Key`**. Esta chave será inicializada e usada para assinar ou verificar os tokens JWT, garantindo que eles sejam autênticos e não tenham sido modificados.

### [Método de Inicialização](#método-de-inicialização)

```java
@PostConstruct
public void init(){
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
}
```
- Anota o método **`init`** com **`@PostConstruct`**, indicando que ele deve ser executado logo após a criação do bean **`JwtUtil`** e a injeção de todas as dependências necessárias. O método converte a **`secret`** em um array de bytes e utiliza o método **`Keys.hmacShaKeyFor`** para gerar uma chave HMAC SHA a partir dela. Essa chave é atribuída à variável **`key`**, preparando-a para uso posterior na assinatura e verificação de tokens.

### [Obtenção de Todas as Reivindicações de um Token](#obtenção-de-todas-as-reivindicações-de-um-token)

```java
public Claims getAllClaimsFromToken(String token) {
    return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
}
```
- Define um método público **`getAllClaimsFromToken`** que recebe um token JWT como argumento e retorna as reivindicações (**`Claims`**) contidas nele. O método usa a chave **`key`** para configurar um construtor de parser do JJWT, analisa o token e extrai o corpo do token, que contém as reivindicações.

### [Verificação de Expiração do Token](#verificação-de-expiração-do-token)

```java

private boolean isTokenExpired(String token) {
    return this.getAllClaimsFromToken(token).getExpiration().before(new Date());
}
```
- Um método privado **`isTokenExpired`** verifica se o token JWT fornecido expirou. Ele faz isso obtendo as reivindicações do token e comparando a data de expiração (**`getExpiration()`**) com a data e hora atuais. Se a data de expiração for anterior à data atual, o método retorna **`true`**, indicando que o token expirou.

### [Verificação de Validade do Token](#verificação-de-validade-do-token)

```java
public boolean isInvalid(String token) {
    return this.isTokenExpired(token);
}
```
- Um método público **`isInvalid`** que usa o método **`isTokenExpired`** para determinar se um token JWT é inválido devido à expiração. Retorna **`true`** se o token estiver expirado (e, portanto, inválido) ou **`false`** caso contrário.

## [Classe AuthenticationFilter](#classe-authenticationfilter)

- A classe AuthenticationFilter é um componente do Spring Cloud Gateway que implementa um filtro de autenticação para requisições HTTP. Vamos analisar cada parte dessa classe para entender como ela funciona.

### [Implementação da Interface GatewayFilter](#implementação-da-interface-gatewayfilter)

```java
public class AuthenticationFilter implements GatewayFilter {}
```
- A classe implementa a interface **`GatewayFilter`** do Spring Cloud Gateway. Isso a define como um filtro que pode ser aplicado a rotas configuradas no gateway.

### [Dependências](#dependências)

```java
private final RouterValidator routerValidator;
private final JwtUtil jwtUtil;
```
- A classe tem duas dependências principais: **`RouterValidator`** e **`JwtUtil`**. **`RouterValidator`** é usado para determinar se uma determinada requisição deve passar pelo processo de autenticação. **`JwtUtil`** é usado para operações relacionadas ao processamento de tokens JWT.

### [Construtor com Injeção de Dependência](#construtor-com-injeção-de-dependência)

```java
@Autowired
public AuthenticationFilter(RouterValidator routerValidator, JwtUtil jwtUtil) {
    this.routerValidator = routerValidator;
    this.jwtUtil = jwtUtil;
}
```

- O construtor da classe utiliza a anotação **`@Autowired`** para injeção de dependência, garantindo que as instâncias de **`RouterValidator`** e **`JwtUtil`** sejam fornecidas pelo Spring no momento da criação do bean **`AuthenticationFilter`**.

### [Implementação do Método filter](#implementação-do-método-filter)

```java
@Override
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
```
- O método **`filter`** é a implementação do contrato definido pela interface **`GatewayFilter`**. Este método é invocado para cada requisição que passa pelo gateway.

### Lógica de Filtragem

```java
ServerHttpRequest request = exchange.getRequest();

if (routerValidator.isSecured.test(request)) {
        if (this.isAuthMissing(request)) {
        return this.onError(exchange, HttpStatus.UNAUTHORIZED);
    }

final String token = this.getAuthHeader(request).substring("Bearer ".length());

    if (jwtUtil.isInvalid(token)) {
        return this.onError(exchange, HttpStatus.FORBIDDEN);
    }

            this.updateRequest(exchange, token);
}
        return chain.filter(exchange);

```

- A lógica do método **`filter`** primeiro verifica se a requisição deve ser segurada usando o **`routerValidator`**. Se a autenticação estiver faltando (**`isAuthMissing`**), retorna um erro 401 (Não Autorizado). Se o token for inválido (**`jwtUtil.isInvalid`**), retorna um erro 403 (Proibido). Se o token for válido, atualiza a requisição com as informações do token e permite que a requisição continue para o próximo filtro no **`chain`**.

### [Métodos Auxiliares](#métodos-auxiliares)

- **`onError`**: Configura uma resposta com o status HTTP específico e finaliza o processamento da requisição.
- **`getAuthHeader`**: Obtém o valor do cabeçalho de autorização da requisição.
- **`isAuthMissing`**: Verifica se o cabeçalho de autorização está ausente na requisição.
- **`updateRequest`**: Atualiza a requisição com informações extraídas do token JWT, como o papel do usuário.


## [Classe GatewayConfig](#classe-gatewayconfig)

A classe GatewayConfig é definida para configurar as rotas no Spring Cloud Gateway, incluindo a aplicação de filtros de autenticação nas rotas específicas. Esta classe desempenha um papel crucial na definição de como as requisições são roteadas para os microserviços e como a segurança é imposta nas rotas. Vamos detalhar cada parte da classe:

### [Classe GatewayConfig](#classe-gatewayconfig)

```java
public GatewayConfig(AuthenticationFilter filter) {
    this.filter = filter;
}
```
- O construtor da classe **`GatewayConfig`** utiliza injeção de dependência para obter uma instância do **`AuthenticationFilter`**. Isso é feito para que o filtro possa ser aplicado às rotas configuradas nesta classe.

### [Bean RouteLocator](#bean-routelocator)

```java
@Bean
public RouteLocator routes(RouteLocatorBuilder builder) {
```
- Este método **`routes`** é anotado com **`@Bean`**, indicando que o objeto retornado por ele deve ser registrado como um bean no contexto do aplicativo Spring. O método configura as rotas para o Spring Cloud Gateway usando um **`RouteLocatorBuilder`** fornecido.


### [Configuração das Rotas](#configuração-das-rotas)

```java
return builder.routes()
        .route(r -> r.path("/produtos/**")
                    .filters(f -> f.filter(filter))
        .uri("http://localhost:3001"))

        .route(r -> r.path("/auth/**")
                    .filters(f -> f.filter(filter))
        .uri("http://localhost:3002"))
        .build();
```
- Dentro do método **`routes`**, as rotas são configuradas usando o **`RouteLocatorBuilder`**. Duas rotas são definidas neste exemplo:
    1. **Rota de Produtos**: Para qualquer caminho que corresponda a **`/produtos/**`**, o **`AuthenticationFilter`** é aplicado como um filtro antes de encaminhar a requisição para o URI **`http://localhost:3001`**. Isso significa que todas as requisições para o caminho **`/produtos/**`** serão autenticadas usando o filtro.
    2. **Rota de Autenticação**: Para qualquer caminho que corresponda a **`/auth/**`**, o mesmo **`AuthenticationFilter`** é aplicado, e as requisições são encaminhadas para o URI **`http://localhost:3002`**. Note que, dependendo da lógica dentro do **`AuthenticationFilter`** e das configurações do **`RouterValidator`**, as requisições para **`/auth/**`** podem ser tratadas de maneira diferente em termos de autenticação.

- O método **`build()`** finaliza a construção do **`RouteLocator`**, que define as rotas configuradas.