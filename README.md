# Floresta Viva + Dark Fantasy — Android

O mesmo aplicativo mantém dois wallpapers animados: Floresta Viva e Cavaleiro Amaldiçoado.
O identificador do aplicativo permanece `br.com.florestaviva.livewallpaper`.

## Cavaleiro — versão 0.7.1

- **Sem animação de respiração:** peitoral, torso e armadura ficam estáticos conforme a arte original.
- A **capa** continua com 24 poses de substituição e interpolação entre quadros.
- Permanecem os efeitos de espada, olhos, velas, névoa, cinzas, inclinação e Despertar.
- O controle de configuração agora chama a opção de **Capa ao vento**.
- O atlas de respiração e seu código de carregamento foram removidos do APK.
- O gerador `tools/generate_replacement_atlases.py` produz exclusivamente a animação da capa.
- O teste `qa/verify_build.py` impede a reintrodução acidental da respiração.

## Compilação e testes

A ação GitHub `.github/workflows/android.yml` gera o atlas da capa, executa a verificação gráfica e compila o APK com Android Lint.

Compilação local: Java 17, Android SDK 35 e Gradle 8.10.2, executar `cd android && gradle lintDebug assembleDebug`.

A fluidez, desempenho e aparência no celular precisam ser confirmados em teste no dispositivo; compilação bem-sucedida não significa aprovação visual.
