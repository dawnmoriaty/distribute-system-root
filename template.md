---
tags:
  - git
  - commit
  - template
  - devops
  - cheatsheet
aliases:
  - Git Commit Templates
  - Commit Commands
created: 2024-05-20
updated: 2024-05-20
cssclasses:
  - git-commit-templates
  - colorful-admonitions
---

# 🎯 Git Commit Templates - Copy & Use

> [!abstract]+ 📌 Cách Dùng
> 1. Tìm template theo loại commit bạn cần
> 2. **Copy command**
> 3. **Paste vào terminal** và chỉnh sửa
> 4. Enter - Xong!

---

## 🚀 QUICK COMMANDS

> [!success]+ ⚡ Copy Nhanh - Dùng Luôn

### Feature Mới
```bash
git commit -m "feat: add user authentication with JWT"
```

### Sửa Bug
```bash
git commit -m "fix: resolve login timeout issue"
```

### Documentation
```bash
git commit -m "docs: update API documentation"
```

### Style/Format
```bash
git commit -m "style: format code with prettier"
```

### Refactor
```bash
git commit -m "refactor: simplify database queries"
```

### Performance
```bash
git commit -m "perf: optimize image loading speed"
```

### Test
```bash
git commit -m "test: add unit tests for auth module"
```

### Chore
```bash
git commit -m "chore: update dependencies to latest"
```

---

## 📦 CHI TIẾT THEO LOẠI

### 🎉 FEAT - Tính Năng Mới

> [!example]+ Khi Nào Dùng
> - Thêm feature mới cho user
> - Functionality mới
> - UI components mới

**Template đơn giản:**
```bash
git commit -m "feat(scope): add [tên feature]"
```

**Template đầy đủ:**
```bash
git commit -m "feat(auth): add Google OAuth2 login" -m "Implement OAuth2 for quick sign-in" -m "Closes #123"
```

**Ví dụ thực tế:**
```bash
git commit -m "feat(cart): add discount coupon system" -m "Users can apply discount codes at checkout" -m "Fixes #456"
```

---

### 🐛 FIX - Sửa Lỗi

> [!bug]+ Khi Nào Dùng
> - Sửa bug
> - Fix logic error
> - Resolve crash/issues

**Template đơn giản:**
```bash
git commit -m "fix(scope): resolve [vấn đề]"
```

**Template đầy đủ:**
```bash
git commit -m "fix(ui): resolve mobile overflow issue" -m "Content overflowed on small screens" -m "Fixes #789"
```

**Ví dụ thực tế:**
```bash
git commit -m "fix(api): fix null pointer in user service" -m "Added null check before accessing user data" -m "Fixes #234"
```

---

### 📄 DOCS - Tài Liệu

> [!note]+ Khi Nào Dùng
> - Update README
> - Thêm comments
> - Change documentation

**Template:**
```bash
git commit -m "docs: update [phần tài liệu]"
```

**Ví dụ:**
```bash
git commit -m "docs: add installation guide" 
git commit -m "docs(readme): add usage examples"
git commit -m "docs: update API endpoints documentation"
```

---

### 🎨 STYLE - Format Code

> [!tip]+ Khi Nào Dùng
> - Format whitespace
> - Add/remove semicolons
> - Code styling (no logic change)

**Template:**
```bash
git commit -m "style: [mô tả thay đổi]"
```

**Ví dụ:**
```bash
git commit -m "style: remove trailing whitespace"
git commit -m "style: add missing semicolons"
git commit -m "style: format with prettier"
```

---

### ♻️ REFACTOR - Tái Cấu Trúc

> [!quote]+ Khi Nào Dùng
> - Refactor code
> - Improve structure
> - No behavior change

**Template:**
```bash
git commit -m "refactor(scope): [mô tả]"
```

**Ví dụ:**
```bash
git commit -m "refactor(auth): simplify token validation"
git commit -m "refactor(db): extract query builder"
```

---

### ⚡ PERF - Tối Ưu Hiệu Năng

> [!warning]+ Khi Nào Dùng
> - Performance improvement
> - Speed optimization
> - Memory optimization

**Template:**
```bash
git commit -m "perf(scope): improve [thứ được optimize]"
```

**Ví dụ:**
```bash
git commit -m "perf: optimize image loading by 40%"
git commit -m "perf(db): add index to user table"
```

---

### ✅ TEST - Test Cases

> [!success]+ Khi Nào Dùng
> - Add tests
> - Update tests
> - Fix test cases

**Template:**
```bash
git commit -m "test(scope): add/test [tên test]"
```

**Ví dụ:**
```bash
git commit -m "test: add unit tests for auth service"
git commit -m "test(api): add integration tests"
```

---

### 🔧 CHORE - Công Cụ

> [!failure]+ Khi Nào Dùng
> - Update dependencies
> - Build process
> - Config files

**Template:**
```bash
git commit -m "chore: [công việc]"
```

**Ví dụ:**
```bash
git commit -m "chore: update npm dependencies"
git commit -m "chore: add eslint config"
git commit -m "chore: update .gitignore"
```

---

## 🎓 TEMPLATE NÂNG CAO

> [!example]+ Commit Với Body & Footer

**Cấu trúc:**
```bash
git commit -m "type: subject" -m "Body line 1" -m "Body line 2" -m "Footer: Closes #123"
```

**Ví dụ đầy đủ:**
```bash
git commit -m "feat(auth): add password reset feature" \
  -m "Allow users to reset password via email" \
  -m "Added email template and token generation" \
  -m "Closes #567"
```

---

## 📋 CHEATSHEET NHANH

> [!abstract]+ Reference Table

| Lệnh | Khi nào dùng |
|------|--------------|
| `git commit -m "feat: ..."` | Tính năng mới |
| `git commit -m "fix: ..."` | Sửa bug |
| `git commit -m "docs: ..."` | Tài liệu |
| `git commit -m "style: ..."` | Format code |
| `git commit -m "refactor: ..."` | Refactor |
| `git commit -m "perf: ..."` | Performance |
| `git commit -m "test: ..."` | Test |
| `git commit -m "chore: ..."` | Tools/Build |

---

## 💡 MẸO HỮU ÍCH

> [!tip]+ Pro Tips

**1. Commit nhiều lần:**
```bash
git add .
git commit -m "feat: add feature"
git commit --amend -m "feat: add feature (updated)"
```

**2. Commit với scope:**
```bash
git commit -m "feat(auth): add login"
git commit -m "fix(ui): fix button"
```

**3. Commit với breaking change:**
```bash
git commit -m "feat: new API" -m "BREAKING CHANGE: old API removed"
```

---

## 🔗 Links

- [[Git Workflow]]
- [[Code Review Guide]]
- [Conventional Commits](https://conventionalcommits.org)

---

*Quick Access: #git #commit #template #devops*

