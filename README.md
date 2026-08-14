# L2jMobius - An Open-Source Server Emulator

## Features

- Multiple chronicle support (early through latest clients)
- Community-driven bug fixes and improvements
- Active development and regular updates
- Full database-driven game mechanics
- Extensive configuration options

---

## Important Legal Notice

**L2jMobius is an open-source software project created through legal reverse engineering methods. This document explains the legal basis for the project. However, this is NOT legal advice. Users should consult with a qualified attorney before using this software, especially for public server operation.**

**Trademark notice:** L2jMobius is an independent project. It is not affiliated with, endorsed by, or sponsored by any game publisher. All trademarks referenced or implied are the property of their respective owners and are used, if at all, solely to identify software with which this project is interoperable.

## Introduction

L2jMobius is an independent, open-source server emulator developed by volunteer contributors. The project's code is licensed open-source and developed in the open.

**Our originality policy:**
All contributions to L2jMobius must be the contributor's original work. We do not accept decompiled code, leaked proprietary code, or code copied from any proprietary implementation. Contributions found to violate this policy are removed from the repository.

**Development Model:**
L2jMobius operates on an open-source development model with public releases made available three times per year. Contributors who actively share code improvements receive early access to ongoing development work. Voluntary donations support project infrastructure, with donors receiving temporary early access as a thank-you gesture. All code becomes publicly available to everyone at no cost.

**Important Distinction:**
- **The L2jMobius project itself is legal** - Creating and sharing server emulator code through reverse engineering.
- **How individuals use the software varies** - Operating public game servers may have different legal considerations depending on jurisdiction and how they're run.

**What we provide:**
- Original source code for server functionality.
- Configuration and data files describing game mechanics (rules, formulas, and numeric values), which are functional in nature.
- Educational resources about server architecture.
- A collaborative development community.

**What we do NOT provide:**
- Game client software.
- Game assets (models, textures, sounds, artwork).
- Any copyrighted content from the original game.
- Links to download copyrighted materials.
- Legal advice for server operators.

**License:** New and rewritten code is licensed under the MIT License. The project is being systematically rewritten under MIT and this will eventually cover the entire codebase. See the License section below.

---

## How L2jMobius is Legal

### 1. Original Code Policy

L2jMobius requires that all code in the repository be the original work of its contributors. This means:

- Contributors own the copyright to their own code and license it to the project.
- Decompiled, leaked, or copied proprietary code is prohibited and removed when found.
- The project maintains this standard through ongoing code review.

**Legal principle:** Independently created code is legally distinct from the original game's code, even if it produces similar results. Copyright protects specific expression, not functionality.

### 2. We Follow Interoperability-Focused Reverse Engineering

Our development methodology is the well-established approach used throughout the software industry:

**How it works:**
1. **Observe:** Watch how the client and server communicate (network packets, protocols).
2. **Document:** Write down what is observed (data formats, message structures).
3. **Implement:** Write new code based on those observations.

**What is prohibited:**
- Accessing or copying proprietary server source code.
- Decompiling server binaries.
- Using leaked or stolen code.
- Copying any existing proprietary implementation.

**Legal precedent:** Reverse engineering for interoperability has been upheld in courts for over 30 years.

### 3. The Law Explicitly Protects Reverse Engineering for Interoperability

**United States - 17 U.S.C. § 1201(f):**
Congress wrote into law that you CAN reverse engineer software to figure out how to make programs work together (interoperability), subject to the conditions in the statute.

**European Union - Software Directive Article 6:**
EU law states that reverse engineering to achieve interoperability is legal and contracts cannot take away this right.

**Many other countries have similar laws:** Canada, Japan, Australia, South Korea and most developed nations protect reverse engineering for compatibility.

### 4. Network Protocols and Functional Elements Aren't Copyrightable

Copyright law protects creative expression, NOT:
- How things work (methods and processes).
- Network communication protocols.
- Data formats and structures.
- Game rules and mechanics.
- System interfaces.

**Example:** You can't copyright the rules of chess, only a specific book explaining chess. Similarly, you can't copyright how a server communicates with a client, only the specific code that does it.

**Legal basis:** U.S. Copyright Law, 17 U.S.C. § 102(b) explicitly excludes "any idea, procedure, process, system, method of operation" from copyright protection. This is also the basis on which our game data files describe mechanics, formulas, and numeric values rather than reproducing any creative content.

### 5. This is How the Entire Software Industry Works

Legal server emulators and reimplementations are everywhere:

**Operating Systems:**
- **FreeBSD/OpenBSD** - Unix-like systems.
- **Linux** - Reimplemented Unix functionality.
- **ReactOS** - Reimplements Windows (20+ years of development).

**Compatibility Software:**
- **Samba** - Windows network compatibility for Linux.
- **Wine** - Runs Windows programs on Linux (30+ years).

**Programming Environments:**
- **Mono** - Open-source .NET implementation.
- **OpenJDK** - Open-source Java (now the official version!).

**Game Emulators:**
- **Dolphin** - GameCube/Wii emulator.
- **PCSX2** - PlayStation 2 emulator.
- **RPCS3** - PlayStation 3 emulator.

**Game Engine Reimplementations:**
- **OpenMW** - Morrowind engine.
- **OpenTTD** - Transport Tycoon engine.
- **ScummVM** - LucasArts adventure games.

**Other MMORPG Server Emulators:**
- **EQEmu** - EverQuest (published by Sony Online Entertainment).
- **MaNGOS/TrinityCore** - World of Warcraft (published by Blizzard Entertainment).
- **Various others** - Ultima Online, RuneScape, etc.

These projects rely on the same legal principles L2jMobius does.

---

## Key Court Cases That Protect Projects Like Ours

### Sega v. Accolade (1992)
**What happened:** Accolade reverse-engineered Sega's console to make compatible games without a license.

**Court's ruling:** Reverse engineering to understand how to make compatible software is **legal and protected as fair use**. The court specifically said that when reverse engineering is the only way to access functional information needed for compatibility, it's lawful.

**Why it matters:** This established that making compatible products through reverse engineering is legal, not copyright infringement.

### Sony v. Connectix (2000)
**What happened:** Connectix created a PlayStation emulator by reverse-engineering the PlayStation BIOS.

**Court's ruling:** Creating an emulator through reverse engineering is **legal**. Even though they made temporary copies during development, the final product (which contained no Sony code) was lawful.

**Why it matters:** Direct precedent that game emulators created through reverse engineering are legal.

### Google v. Oracle (2021)
**What happened:** Google copied Java API declarations for Android to let programmers use their existing Java knowledge.

**Court's ruling:** The Supreme Court ruled this was **fair use**, emphasizing that functional elements used for interoperability and enabling developers to use their knowledge receives special protection.

**Why it matters:** Most recent Supreme Court case affirming that functional compatibility receives strong fair use protection.

---

## What About EULAs and Terms of Service?

**The Reality:** Yes, running a private server probably violates the game's Terms of Service.

**But here's what that means legally:**

### EULA Violations Are NOT Copyright Infringement

- **Contract vs. Copyright:** Breaking a contract (EULA) is different from breaking copyright law.
- **Who it applies to:** EULAs only bind people who agreed to them.
- **What it means:** Publishers can ban your accounts, but that's not a criminal matter.

### The Law Overrides Contracts in Many Places

**European Union:** The Software Directive explicitly states that contracts cannot override the right to reverse engineer for interoperability. Those contract terms are "null and void."

**United States:** More complex, but many courts have held that statutory rights (like the DMCA's reverse engineering exception) cannot be eliminated by private contracts.

### Not Everyone Agreed to the EULA

- Contributors who never played the game are not bound by its EULA.
- Observing network traffic doesn't require agreeing to terms.
- Information obtained lawfully by non-parties is not "tainted".

### Even If Challenged, Fair Use Applies

Even if someone argued that some elements were copyrightable (which we dispute), our use would still be **fair use** under copyright law.

Fair use considers four factors:

### 1. Purpose and Character of Use
✓ **Educational** - Teaching server architecture, networking, programming.  
✓ **Research** - Academic study of game systems.  
✓ **Preservation** - Maintaining access to legacy game versions.  
✓ **Transformative** - Used for learning and research, not just playing.  
✓ **Freely available** - All code is released publicly at no cost.  

### 2. Nature of Copyrighted Work
✓ **Highly functional** - Server software is functional, not creative.  
✓ **Published** - Game is publicly available.  
✓ **Less protection** - Functional works get less copyright protection.  

### 3. Amount Used
✓ **No verbatim copying** - Copying proprietary code is prohibited.  
✓ **Only functional specs** - Just what's necessary for compatibility.  
✓ **Original implementation** - Server functionality is independently implemented.  

### 4. Market Effect
✓ **No substitution** - Users still need legitimate game client.  
✓ **Potential benefits** - Extends product life, maintains community.  
✓ **No harm to current sales** - Often used for deprecated versions.  
✓ **Competition is lawful** - Courts have said competition through interoperability is legal, not infringement.  

**Legal precedent:** Courts have consistently found that reverse engineering for compatibility has minimal market impact and is protected.

---

## Why We Don't Distribute Game Assets

We maintain **strict separation** between our code and copyrighted game content:

**We NEVER provide:**
- Game client software.
- 3D models or textures.
- Sounds or music.
- Artwork or animations.
- Copyrighted creative content of any kind.
- Links to download any of the above.

**Our data files:**
The server's configuration and data files describe game mechanics - rules, formulas, stats, and numeric values. These are functional specifications, not creative works, and functional elements are excluded from copyright protection (17 U.S.C. § 102(b)). We do not include creative content such as artwork, audio, models, or narrative text.

**Users must:**
- Obtain a legitimate copy of the game client themselves.
- Accept responsibility for their own compliance with the client's license.
- Understand that they may be violating Terms of Service by connecting to unofficial servers.

**This separation is exactly like:**
- Emulators requiring users to provide their own game ROMs.
- Linux requiring users to provide proprietary firmware.
- Wine requiring users to provide Windows software.

**Legal principle:** The emulator itself doesn't infringe. Users must comply with licenses for the client software they independently obtain.

---

## Educational and Research Value

L2jMobius serves important purposes protected by law:

**Educational Uses:**
- Teaching server architecture and design.
- Learning network programming and protocols.
- Studying database design and optimization.
- Understanding client-server architectures.
- Training in multi-threaded programming.

**Research Uses:**
- Academic study of MMORPG mechanics.
- Research into virtual economies.
- Security research and analysis.
- Network protocol documentation.

**Preservation:**
- Maintaining knowledge of legacy systems.
- Documenting game version history.
- Preserving cultural heritage of digital entertainment.

**Legal protection:** Copyright law explicitly protects educational and research uses. U.S. law lists "teaching, scholarship, or research" as examples of fair use.

---

## Our Commitment to Legality

### What We Do to Stay Legal

1. **Originality Requirement:** All contributions must be the contributor's original work. Decompiled, leaked, or copied proprietary code is prohibited.
2. **Ongoing Code Review:** Contributions are reviewed, and code found to violate the originality requirement is removed.
3. **No Asset Distribution:** Strict policy against distributing copyrighted assets, enforced in the repository and the community.
4. **Educational Focus:** Emphasizing research, education and preservation.
5. **Transparency:** Fully open-source with public development.
6. **Responsive:** We address legitimate legal concerns promptly. Rights holders can contact the project through the forum, and valid takedown requests will be honored.

### Community Standards

We expect all contributors and users to:
- Never distribute copyrighted game assets.
- Never modify or redistribute the game client.
- Respect intellectual property rights.
- Use the software responsibly and legally.
- Report any compliance concerns.
- Contribute in good faith.

---

## International Perspective

### Strong Legal Protection Countries

**United States:**
- DMCA § 1201(f) protects reverse engineering for interoperability.
- Fair use doctrine.
- First Amendment protections for code.
- Strong precedent (Sega, Sony, Google cases).

**European Union:**
- Software Directive mandatory exceptions.
- Contracts cannot override interoperability rights.
- Competition law supports compatibility.
- Recent court decisions expanding protections.

**Other Countries with Good Protection:**
- Canada - Strong reverse engineering rights.
- Australia - Copyright Act protections.
- Japan - Interoperability exceptions.
- South Korea - Legal reverse engineering framework.

### Countries with Less Clear Laws

Some jurisdictions have less developed case law or different legal frameworks. **If you're in a country not listed above, consult local legal counsel before using this software.**

---

## License

**L2jMobius is moving to the MIT License.**

All new code and all rewritten code is licensed under the MIT License. The project is being systematically rewritten under MIT, and this effort will eventually cover the entire codebase.

Code that has not yet been rewritten remains under GPLv3. **The license header in each file governs that file.** Until the rewrite is complete, distributions containing GPLv3 files must be treated as GPLv3-bound as a whole. If you intend to reuse L2jMobius code under MIT terms, verify the headers of the specific files you use.

```
MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense and/or sell
copies of the Software and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```

---

## Contributing

We welcome contributions from the community!

**Our Development Model:**
- **Public releases** - Code made publicly available three times per year.
- **Active development** - Ongoing work accessible to contributors and supporters.
- **Contributor access** - Those who share code improvements get early access to development.
- **Supporter access** - Voluntary donations support infrastructure; donors receive temporary early access as appreciation.
- **Always eventually free** - All code becomes publicly available.

**Contribution requirements - all contributions must be original code. Never submit:**
- Decompiled proprietary code.
- Leaked server files.
- Copyrighted game assets.

By submitting a contribution, you certify that it is your own original work and that you have the right to license it to the project.

**How to contribute:**
- Report bugs and issues on our forum.
- Submit code improvements and bug fixes.
- Help with documentation and testing.
- Share your knowledge with other developers.

**For server operators:**
- Understand that operating public servers may violate game Terms of Service.
- This is a separate issue from the legality of the emulator code itself.
- Commercial server operation carries additional legal considerations.
- You are responsible for your own compliance with local laws.
- L2jMobius developers are not responsible for how users deploy the software.

**Project policy:** We do not encourage or support commercial server operation. Our project exists for education, research, preservation and collaborative development.

## Support & Community

- **Forum:** Get help, share ideas and discuss development.
- **Discord:** Real-time chat with developers and users.

---

## Disclaimer

**THIS IS NOT LEGAL ADVICE.**

This software is provided "as is" without warranty of any kind.

**About the L2jMobius Code:**
- The L2jMobius emulator code is created through legal reverse engineering methods.
- We believe the code is legal based on established precedent and statutory protections.
- We distribute only project code and functional data, never copyrighted game assets.

**About Using This Software:**
Users are responsible for ensuring their use complies with applicable laws in their jurisdiction.

**Important distinctions:**
- **Creating emulator code** (what L2jMobius does) is protected by reverse engineering laws.
- **Operating game servers** (what a user can do) may violate Terms of Service and raise different legal issues.
- **Commercial server operation** is particularly legally complex and not encouraged by this project.

**The developers and contributors:**
- Make no guarantees about legality in all jurisdictions.
- Are not responsible for how third parties use this software.
- Disclaim all liability for legal consequences of server operation.
- Do not encourage commercial server operation.
- Recommend this software for educational, research and preservation purposes.
- Strongly recommend consulting qualified legal counsel before operating any public servers.

**If you operate servers:**
- Understand you may be violating game Terms of Service (contract issue).
- Terms of Service violations can result in account bans.
- Commercial operation may face additional legal scrutiny.
- You accept all legal risks and responsibilities.
- Consult an attorney in your jurisdiction.

Laws vary by country and state. **When in doubt, consult a qualified attorney in your jurisdiction.**

---

## Thank you!

Thanks to all the people that helped with the development and contributed over the years.
