# Contributing to Multi-Platform Auto-Healing Framework

Thank you for your interest in contributing to the Multi-Platform Auto-Healing Framework! This document provides guidelines and information for contributors.

## 🚀 Getting Started

### Prerequisites
- Java 11 or higher
- Maven 3.6 or higher
- Git
- IDE with Maven support (IntelliJ IDEA, Eclipse, VS Code)

### Setting Up Development Environment

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/multi-platform-autohealing.git
   cd multi-platform-autohealing
   ```

2. **Build the project**
   ```bash
   mvn clean compile
   ```

3. **Run tests**
   ```bash
   mvn test
   ```

4. **Generate reports**
   ```bash
   mvn site
   ```

## 📋 Development Guidelines

### Code Style
- Follow Google Java Style Guide
- Use meaningful variable and method names
- Add JavaDoc comments for public APIs
- Maintain 80% code coverage for new features

### Project Structure
```
src/
├── main/java/com/autohealing/
│   ├── core/           # Core healing engine
│   ├── adapters/       # Platform adapters
│   ├── strategies/     # Healing strategies
│   ├── config/         # Configuration management
│   ├── reporting/      # Reporting and analytics
│   └── examples/       # Usage examples
├── main/resources/     # Configuration files
└── test/java/          # Unit and integration tests
```

### Adding New Platform Support

1. **Create Platform Adapter**
   ```java
   public class MyPlatformAdapter implements PlatformAdapter {
       @Override
       public String getPlatformType() {
           return "MY_PLATFORM";
       }
       
       // Implement other required methods...
   }
   ```

2. **Add Platform-Specific Tests**
   ```java
   public class MyPlatformAdapterTest {
       @Test
       public void testFindElement() {
           // Test implementation
       }
   }
   ```

3. **Update Documentation**
   - Add platform to README.md
   - Create usage example
   - Update configuration documentation

### Adding New Healing Strategy

1. **Implement HealingStrategy Interface**
   ```java
   public class MyHealingStrategy implements HealingStrategy {
       @Override
       public boolean canHandle(String platformType) {
           return supportedPlatforms.contains(platformType);
       }
       
       @Override
       public <T> T heal(PlatformAdapter adapter, String elementId, 
                        String originalLocator, Class<T> expectedType, Object context) {
           // Healing logic
       }
       
       // Implement other methods...
   }
   ```

2. **Add Strategy Tests**
   ```java
   public class MyHealingStrategyTest {
       @Test
       public void testSuccessfulHealing() {
           // Test successful healing scenario
       }
       
       @Test
       public void testFailedHealing() {
           // Test failed healing scenario
       }
   }
   ```

3. **Register Strategy in Framework**
   ```java
   // In AutoHealingFramework.addHealingStrategies()
   MyHealingStrategy myStrategy = new MyHealingStrategy();
   myStrategy.initialize(configuration.getAllConfiguration());
   engine.addHealingStrategy(myStrategy);
   ```

## 🧪 Testing Guidelines

### Unit Tests
- Test all public methods
- Mock external dependencies
- Use descriptive test names
- Follow AAA pattern (Arrange, Act, Assert)

### Integration Tests
- Test end-to-end scenarios
- Use real browser/application instances when possible
- Test cross-platform compatibility

### Test Categories
```java
@Category(UnitTest.class)
public class FastTest { }

@Category(IntegrationTest.class)  
public class SlowTest { }

@Category(PlatformSpecific.class)
public class WindowsOnlyTest { }
```

### Running Specific Tests
```bash
# Unit tests only
mvn test -Dgroups="com.autohealing.categories.UnitTest"

# Integration tests
mvn test -Dgroups="com.autohealing.categories.IntegrationTest"

# Platform-specific tests
mvn test -Dgroups="com.autohealing.categories.WindowsTest"
```

## 📝 Documentation

### Code Documentation
- All public APIs must have JavaDoc
- Include usage examples in JavaDoc
- Document complex algorithms and decisions

### README Updates
- Add new features to feature list
- Update quick start examples
- Add configuration options

### Wiki Contributions
- Create detailed tutorials
- Add troubleshooting guides
- Share best practices

## 🔄 Pull Request Process

### Before Submitting
1. **Create feature branch**
   ```bash
   git checkout -b feature/my-amazing-feature
   ```

2. **Make changes**
   - Write code with tests
   - Update documentation
   - Ensure all tests pass

3. **Commit changes**
   ```bash
   git add .
   git commit -m "feat: add amazing feature for better healing"
   ```

4. **Push to fork**
   ```bash
   git push origin feature/my-amazing-feature
   ```

### PR Requirements
- [ ] All tests pass
- [ ] Code coverage maintained or improved  
- [ ] Documentation updated
- [ ] No breaking changes (or clearly documented)
- [ ] Follows code style guidelines

### PR Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added/updated
```

## 🐛 Bug Reports

### Before Reporting
1. Check existing issues
2. Ensure you're using latest version
3. Try to reproduce with minimal example

### Bug Report Template
```markdown
**Bug Description**
Clear description of the bug

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. See error

**Expected Behavior**
What you expected to happen

**Screenshots/Logs**
Add screenshots or log files if applicable

**Environment:**
- OS: [e.g. Windows 10]
- Java Version: [e.g. 11.0.2]
- Framework Version: [e.g. 1.0.0]
- Browser/Application: [if applicable]

**Additional Context**
Any other context about the problem
```

## 💡 Feature Requests

### Feature Request Template
```markdown
**Feature Description**
Clear description of the feature

**Use Case**
Why is this feature needed? What problem does it solve?

**Proposed Solution**
How would you like this to work?

**Alternatives Considered**
What alternatives have you considered?

**Platform Impact**
Which platforms would this affect?
```

## 🏆 Recognition

### Contributors
We recognize contributors in:
- README.md contributors section
- Release notes
- Project wiki
- Conference presentations (with permission)

### Contribution Types
- Code contributions
- Documentation improvements
- Bug reports and testing
- Feature suggestions
- Community support

## 📞 Getting Help

### Channels
- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and community support
- **Email**: maintainers@autohealing.com
- **Slack**: [Auto-Healing Community](https://autohealing-community.slack.com)

### Response Times
- **Bugs**: Within 24-48 hours
- **Features**: Within 1 week
- **Questions**: Within 24 hours
- **Security Issues**: Within 12 hours

## 📜 Code of Conduct

### Our Pledge
We pledge to make participation in our project a harassment-free experience for everyone, regardless of age, body size, disability, ethnicity, gender identity and expression, level of experience, nationality, personal appearance, race, religion, or sexual identity and orientation.

### Our Standards
- Use welcoming and inclusive language
- Be respectful of differing viewpoints
- Accept constructive criticism gracefully
- Focus on what is best for the community
- Show empathy towards other community members

### Enforcement
Instances of abusive, harassing, or otherwise unacceptable behavior may be reported to the project maintainers. All complaints will be reviewed and investigated promptly and fairly.

---

Thank you for contributing to the Multi-Platform Auto-Healing Framework! 🎉
