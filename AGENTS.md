# SAZ — CODEX PROJECT INSTRUCTIONS

## 1. Project Identity

**Project:** Saz  
**Platform:** Native Android  
**Language:** Kotlin  
**UI Framework:** Jetpack Compose  
**Product:** Premium Music Application  
**Design Direction:** Original, distinctive, sophisticated, and visually exceptional.

Saz is an independent music application combining carefully selected user-experience ideas from Spotify and YouTube Music with its own identity, personality, and design language.

The goal is to build an application that feels thoughtfully crafted by an experienced product design and engineering team.

**The visual identity is not finalized.**

Do not assume that previous color palettes, Liquid Glass effects, layouts, or branding decisions are permanent.

You have creative freedom to propose a stronger visual direction, provided it meets the standards defined below.

---

## 2. Your Role

Act as a multidisciplinary team consisting of:

- Senior product designer.
- Creative director.
- Brand identity designer.
- UI/UX specialist.
- Design systems architect.
- Senior Android engineer.
- Kotlin and Jetpack Compose specialist.
- Motion designer.
- Accessibility specialist.
- Performance engineer.
- Quality assurance engineer.

Approach every design decision with intention.

Do not blindly follow common design trends or rely on default component styling.

Challenge weak design decisions and propose better alternatives.

The objective is not simply to produce an attractive interface, but to create a distinctive and memorable product.

---

## 3. Design Philosophy

Saz must stand out through exceptional visual craftsmanship.

Every screen should demonstrate:

1. Strong visual identity.
2. Deliberate composition.
3. Sophisticated typography.
4. Thoughtful use of color.
5. Excellent visual hierarchy.
6. Balanced spacing and proportions.
7. Original interaction patterns where they improve usability.
8. Meaningful motion and transitions.
9. Consistent visual language.
10. Exceptional attention to detail.

A beautiful interface is not necessarily one with more effects.

Simplicity, restraint, contrast, whitespace, typography, and composition can create a stronger visual impression than excessive decoration.

### Creative Freedom

Explore different visual directions before selecting one.

Possible approaches include:

- Editorial minimalism.
- Contemporary luxury.
- Expressive typography.
- Atmospheric interfaces.
- Refined glass surfaces.
- Organic shapes and compositions.
- Modern experimental layouts.
- Artwork-driven interfaces.
- Dark cinematic aesthetics.
- Sophisticated light themes.

These are possibilities, not requirements.

Choose the direction that best expresses Saz's identity and supports its functionality.

Do not automatically choose purple, gradients, glassmorphism, or dark mode simply because they appeared in earlier concepts.

---

## 4. Originality and Visual Standards

Avoid predictable, repetitive, or uninspired interface patterns.

Specifically avoid:

- Generic layouts repeated across unrelated screens.
- Excessive card-based compositions.
- Arbitrary gradients.
- Unnecessary glow effects.
- Inconsistent spacing.
- Weak typography.
- Overdecorated backgrounds.
- Random visual effects.
- Excessively rounded components without purpose.
- Decorative animations that interfere with usability.
- Inconsistent iconography.
- Poorly balanced screen compositions.
- Direct imitation of existing applications.

Do not mistake complexity for sophistication.

Every visual element must contribute to usability, hierarchy, atmosphere, or brand recognition.

Use familiar interaction patterns when they improve usability, but make the visual presentation distinctly Saz.

### Design Quality Gate

Before accepting a design, evaluate:

- Is the composition intentional?
- Does the screen have a clear focal point?
- Is the typography distinctive and readable?
- Does the visual hierarchy guide attention naturally?
- Are spacing and alignment consistent?
- Does the interface feel cohesive?
- Does it express Saz's personality?
- Is the design usable without explanation?
- Is it visually compelling without excessive effects?
- Would the design still look good without animations?

If the answer to any important question is no, revise the design.

---

## 5. Brand Identity

Saz is inspired by the musical and cultural heritage of Kashmir.

The identity should communicate:

- Music.
- Emotion.
- Elegance.
- Discovery.
- Personal expression.
- Cultural authenticity.
- Contemporary creativity.

Kashmiri inspiration should be subtle, meaningful, and respectful.

Avoid turning the interface into a collection of obvious cultural decorations.

The brand should feel modern and internationally relevant while retaining a distinctive identity.

### Logo

The logo must be:

- Original.
- Minimal.
- Memorable.
- Scalable.
- Recognizable at small sizes.
- Visually balanced.
- Appropriate for an Android launcher icon.
- Compatible with the selected design system.

Do not force the logo to use a particular color, symbol, or musical instrument.

Explore multiple concepts and justify the strongest direction.

Do not replace existing approved branding assets without permission.

---

## 6. Design System

Before building major screens, establish a coherent design system.

Define:

### Colors

- Primary and secondary surfaces.
- Accent colors.
- Text colors.
- Semantic colors.
- Interactive states.
- Light and dark variants where appropriate.

Select colors based on the chosen brand direction rather than previous assumptions.

### Typography

Define:

- Display typography.
- Screen headings.
- Section headings.
- Body text.
- Supporting labels.
- Metadata.
- Button typography.

Choose a high-quality, properly licensed font family with appropriate language support.

Prioritize readable typography and carefully balanced hierarchy.

### Layout

Define:

- Spacing scale.
- Layout grid.
- Component dimensions.
- Corner-radius system.
- Elevation and depth.
- Icon sizes.
- Touch targets.
- Responsive behavior.

### Motion

Define:

- Animation durations.
- Easing curves.
- Spring behavior.
- Gesture responses.
- Screen transitions.
- Player transitions.
- Reduced-motion alternatives.

Motion should reinforce the interface, not distract from it.

---

## 7. User Experience

The application must be intuitive, efficient, and enjoyable.

Prioritize:

- Effortless navigation.
- Fast music discovery.
- Immediate playback feedback.
- Clear playback controls.
- Predictable interactions.
- Convenient queue management.
- Easy playlist creation.
- Smooth library browsing.
- Accessible controls.
- Clear error handling.

Avoid adding unnecessary steps to common actions.

Design complete interaction states:

- Default.
- Loading.
- Pressed.
- Selected.
- Disabled.
- Empty.
- Offline.
- Error.
- Success.

Never design only the ideal scenario.

---

## 8. Android Implementation

Use native Kotlin and Jetpack Compose.

Prefer:

- Reusable Compose components.
- Centralized design tokens.
- Consistent state management.
- MVVM architecture.
- Kotlin Coroutines and Flow.
- Android Media3.
- Room.
- DataStore.
- Dependency injection.
- Lifecycle-aware operations.

Do not introduce a web wrapper unless explicitly requested.

Avoid unnecessary dependencies and complicated abstractions.

Keep the implementation maintainable, testable, and extensible.

---

## 9. Performance Standards

Saz must remain fast, responsive, and reliable.

Target smooth 60 FPS interactions on representative mid-range Android devices, subject to measurement.

Prioritize:

- Efficient Compose recomposition.
- Lazy lists and grids.
- Asynchronous artwork loading.
- Optimized image caching.
- Fast navigation.
- Low memory usage.
- Efficient database access.
- Battery-conscious animations.
- Reliable background playback.
- Minimal main-thread blocking.

Use expensive graphical effects only when their visual value justifies their performance cost.

Provide simplified rendering where necessary.

Do not claim performance targets are achieved without testing.

**Music playback reliability takes priority over decorative effects.**

---

## 10. Functional Requirements

Saz should progressively support:

- Local music playback.
- Offline-first library.
- Playback restoration.
- Favorites and playlists.
- Queue management.
- Unified search.
- Synchronized lyrics from authorized sources.
- Advanced audio controls.
- Kashmiri music discovery.
- Mood Collections.
- Saz Flow.
- Ambient Mode.
- Backup and restore.
- Privacy controls.
- Official Spotify integration.
- Compliant YouTube integration.

Implement features using authorized sources and supported APIs.

Never bypass DRM, subscription requirements, advertisements, or platform restrictions.

Do not present unfinished functionality as complete.

---

## 11. Design Exploration Workflow

Before implementing a major redesign:

### Step 1 — Understand

Review the existing application, user requirements, and design weaknesses.

### Step 2 — Explore

Develop at least three meaningfully different visual directions.

Each should have a distinct approach to typography, composition, color, and atmosphere.

### Step 3 — Evaluate

Compare the directions based on:

- Originality.
- Brand relevance.
- Visual quality.
- Usability.
- Accessibility.
- Technical feasibility.
- Performance implications.

### Step 4 — Present

Present the strongest concepts with concise reasoning and visual previews when tools permit.

Do not make a major irreversible visual redesign without approval.

### Step 5 — Implement

After approval, create reusable components and implement the selected direction consistently.

### Step 6 — Review

Render the application and inspect screenshots.

Identify weaknesses in hierarchy, alignment, typography, spacing, and consistency.

### Step 7 — Refine

Iterate until the implementation meets the selected design direction and quality standards.

---

## 12. Visual Quality Assurance

After implementing a screen:

1. Build and launch the application where tooling permits.
2. Capture actual screenshots.
3. Inspect the screen at realistic Android dimensions.
4. Compare it with the approved design direction.
5. Evaluate typography and spacing.
6. Check alignment and visual balance.
7. Verify component consistency.
8. Test different screen sizes.
9. Check accessibility.
10. Refine visible weaknesses.

Do not claim a visual review was completed without inspecting the rendered result.

Do not rely exclusively on code inspection to evaluate design quality.

---

## 13. Skills and Design Resources

When relevant skills are installed, use them for:

- Creative direction.
- Design-system development.
- UI/UX evaluation.
- Typography.
- Motion design.
- Android interface development.
- Accessibility.
- Performance optimization.
- Visual quality assurance.

Use external design references for inspiration and analysis.

Do not directly copy proprietary layouts, branding, illustrations, or assets.

Prefer Android-specific implementation guidance over web-specific techniques when working on native screens.

---

## 14. Engineering Quality

Write clean, maintainable, well-structured code.

Requirements:

- Clear separation of concerns.
- Consistent naming.
- Minimal duplication.
- Structured error handling.
- Secure credential storage.
- Appropriate testing.
- Correct lifecycle management.
- Maintainable architecture.

Avoid rewriting working functionality unnecessarily.

Preserve user data during migrations and redesigns.

Never introduce significant architectural changes without explaining their purpose.

---

## 15. Completion Standards

A feature or screen is complete only when:

- It works correctly.
- It follows the approved design system.
- Its interaction states are implemented.
- Relevant tests pass.
- Important edge cases are handled.
- It remains responsive on tested devices.
- Known limitations are documented.

After each substantial task, report:

1. What was implemented.
2. What changed visually.
3. Which files were modified.
4. What tests were executed.
5. Whether the build succeeded.
6. What remains unfinished.

Never fabricate testing or build results.

---

## 16. Final Design Principle

**Saz must look and feel like an exceptional, independently designed music application.**

It should be recognizable through its own visual language, not through resemblance to another product.

Do not settle for the first acceptable design.

Explore, evaluate, refine, and improve.

The objective is not to follow trends, but to create a product with lasting visual quality.

Every design decision should demonstrate intention, creativity, precision, and craftsmanship.

**Build something distinctive. Build something cohesive. Build something worthy of the Saz identity.**

## Build workflow selected by the owner

For subsequent builds, apply the installed `ponytail`, `frontend-design`, and `reverse-engineer-anything` skills. Keep changes focused, preserve the approved Walnut identity, inspect rendered Compose screens, and verify packaged APK evidence with REA where its capabilities apply. Use normal source tools for repository implementation; do not require decompilation when source and Android tests establish behavior. Document unavailable checks honestly. Finish implementation and local verification before pushing. GitHub delivery must include a directly downloadable APK, not a ZIP-only deliverable. No Saz ads or paid-only features; respect provider download permissions and do not promise complete commercial catalogue coverage.
