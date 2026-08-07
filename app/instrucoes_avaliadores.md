# Instruções para Avaliadores do Google Play Store / Instructions for Google Play Store Reviewers

Este documento contém as informações necessárias para testar e avaliar as funcionalidades do aplicativo **InvenTI**.

---

## 🇧🇷 Português (Portuguese)

### 1. Acesso ao Aplicativo
O aplicativo já vem pré-configurado com a URL do servidor GLPI de testes. Você **não** precisa alterar as configurações de URL.

* **Servidor Pré-configurado:** `https://campusparaiso.ifto.edu.br/glpi`
* **Usuário de Teste:** `teste`
* **Senha de Teste:** `Asdf2026@`

### 2. Passos para Avaliação das Funcionalidades
Após realizar o login com as credenciais acima, você poderá testar as seguintes funcionalidades principais:

1. **Pesquisa e Consulta de Equipamentos (Aba Início):**
   - Visualize a lista de computadores e monitores cadastrados no inventário.
   - Use a barra de pesquisa no topo para buscar por nome ou número de série.
   - Selecione um ou mais itens e clique em **Exportar PDF** no menu flutuante inferior para testar a geração automática do painel de etiquetas de inventário com QR Code.
   - Clique em qualquer equipamento para ver detalhes na tela expandida.

2. **Gerenciamento de Chamados (Aba Chamados):**
   - Visualize chamados ativos e resolvidos.
   - Clique no botão flutuante **"+"** para criar um novo chamado (incidente ou requisição).
   - Abra um chamado existente para ver detalhes, adicionar comentários/acompanhamentos ou anexar documentos.

3. **Base de Conhecimento (Aba Base de Conhecimento):**
   - Acesse artigos de ajuda e procedimentos operacionais salvos no GLPI.
   - Use a barra de pesquisa ou filtre por categorias.

4. **Cofre de Senhas (Aba Cofre):**
   - Esta aba demonstra a integração com o gerenciador de segredos **Infisical**.
   - Permite visualizar credenciais vinculadas a computadores ou localidades de forma segura.

---

## 🇺🇸 English (Inglês)

### 1. App Access
The application comes pre-configured with the test GLPI server URL. You **do not** need to change the server URL settings.

* **Pre-configured Server URL:** `https://campusparaiso.ifto.edu.br/glpi`
* **Test Username:** `teste`
* **Test Password:** `Asdf2026@`

### 2. Steps to Evaluate Features
After logging in using the credentials above, you can evaluate the following core features:

1. **Equipment Inventory List & Search (Home Tab):**
   - View the list of registered computers and monitors in the inventory.
   - Use the search bar at the top to filter items by name or serial number.
   - Select one or more items and click **Export PDF** on the bottom floating bar to test generating inventory labels with QR Codes.
   - Tap any item to view its detailed configuration.

2. **Ticket Management (Tickets Tab):**
   - View active, pending, and resolved support tickets.
   - Tap the floating **"+"** button to submit a new ticket (Incident or Request).
   - Open an existing ticket to view details, add follow-ups, or upload attachments.

3. **Knowledge Base (Knowledge Base Tab):**
   - View guides and documentation retrieved from GLPI.
   - Use the search bar or filter articles by category.

4. **Credential Password Vault (Vault Tab):**
   - This tab demonstrates integration with the **Infisical** secrets manager.
   - It allows securely viewing and managing credentials linked to computers or locations.
