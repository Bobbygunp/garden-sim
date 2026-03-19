# References

All external sources consulted during the analysis, design, and implementation of the
Computerized Garden Simulation project are listed below.

---

## Gardening & Horticulture Knowledge

**Plant Environmental Requirements**

- Relf, D., & Ball, E. (2009). *Environmental Requirements for Plant Growth*.
  Virginia Cooperative Extension, Publication 426-724.
  Used for: temperature tolerance ranges for Tomato, Rose, Lettuce, Carrot, Sunflower, Cactus.

- University of California Agriculture & Natural Resources. (2022).
  *Vegetable Research & Information Center — Growing Tomatoes*.
  https://vric.ucdavis.edu/
  Used for: Tomato ideal temperature range (60–85 °F), watering frequency, and nutrient needs.

- Royal Horticultural Society. (2023). *Growing Roses: Soil, Water, and Temperature*.
  https://www.rhs.org.uk/plants/roses/growing-guide
  Used for: Rose ideal temperature (55–80 °F), water needs, and pest vulnerability (aphids).

- Bratsch, A. (2009). *Specialty Crop Profile: Lettuce*. Virginia Cooperative Extension,
  Publication 438-410.
  Used for: Lettuce cold tolerance (ideal 40–70 °F), high water need, susceptibility to pests.

- Bracy, R. P., & Parish, R. L. (1998). *Carrot Production Guide*. Louisiana State University
  AgCenter.
  Used for: Carrot temperature range (45–75 °F) and root vegetable water/nutrient requirements.

- Sunflower Growing Guide. (2021). National Sunflower Association.
  https://www.sunflowernsa.com/growers/
  Used for: Sunflower temperature tolerance (55–91 °F) and high light requirement (10 hrs/day).

- Mauseth, J. D. (2006). *Structure-Function Relationships in Highly Modified Shoots of Cactaceae*.
  Annals of Botany, 98(5), 901–926.
  Used for: Cactus drought tolerance, minimal water needs, and wide temperature range (50–100 °F).

**Pest & Insect Biology**

- Blackman, R. L., & Eastop, V. F. (2000). *Aphids on the World's Crops: An Identification
  and Information Guide* (2nd ed.). Wiley.
  Used for: Aphid behavior, plant damage rates, and lifecycle (lifespan ~200 ticks modeled after
  aphid generation time of ~10–14 days at warm temperatures).

- Capinera, J. L. (2001). *Handbook of Vegetable Pests*. Academic Press.
  Used for: Caterpillar (Lepidoptera larva) feeding damage rates and plant susceptibilities.

- Free, J. B. (1993). *Insect Pollination of Crops* (2nd ed.). Academic Press.
  Used for: Honeybee (Apis mellifera) pollination behavior and movement patterns modeled in
  the Bee class.

- Hodek, I., van Emden, H. F., & Honěk, A. (2012). *Ecology and Behaviour of the Ladybird
  Beetles (Coccinellidae)*. Wiley-Blackwell.
  Used for: Ladybug predation behavior, aphid consumption rates, and biological control
  effectiveness modeled in the Ladybug class.

**Irrigation & Soil Moisture**

- Doorenbos, J., & Pruitt, W. O. (1977). *Guidelines for Predicting Crop Water Requirements*.
  FAO Irrigation and Drainage Paper No. 24. Food and Agriculture Organization of the United Nations.
  Used for: Per-species water consumption rates (waterNeedPerTick values) and threshold-based
  irrigation triggers modeled in WateringSystem.

- Smajstrla, A. G., Boman, B. J., et al. (1990). *Efficiencies of Florida Agricultural Irrigation
  Systems*. University of Florida IFAS Extension, Bulletin 247.
  Used for: Sprinkler system coverage design and zone-based irrigation concepts.

**Plant Nutrition & Fertigation**

- Hochmuth, G. J. (2012). *Plant Nutrient Functions and Deficiency and Toxicity Symptoms*.
  University of Florida IFAS Extension, Circular 792.
  Used for: Nutrient deficiency health penalties (below 10 % nutrients → health loss), nutrient
  burn threshold (above 85 %), and per-species fertilization rates in FertigationSystem.

- Bar-Yosef, B. (1999). *Advances in Fertigation*. Advances in Agronomy, 65, 1–77.
  Used for: Fertigation system design — injecting fertilizer through the irrigation network
  (modeled as nutrients added during each watering cycle in WateringSystem).

**Climate Control & Temperature**

- ASHRAE. (2017). *ASHRAE Handbook — Fundamentals*. American Society of Heating,
  Refrigerating and Air-Conditioning Engineers.
  Used for: Heating/cooling system response modeling, temperature adjustment rates in
  HeatingSystem, and zone-based climate control design.

**Light & Grow Lighting**

- Barta, D. J., Tibbitts, T. W., Bula, R. J., & Morrow, R. C. (1992). *Evaluation of Light
  Emitting Diode Characteristics for a Space-Based Plant Irradiation Source*.
  Advances in Space Research, 12(5), 141–149.
  Used for: Supplemental grow light design and Daily Light Integral (DLI) concept implemented
  in LightingSystem and Plant.lightAccumulator.

- Faust, J. E., & Logan, J. (2018). *Daily Light Integral: A Research Review and High-
  Resolution Maps of the United States*. HortScience, 53(9), 1250–1257.
  Used for: DLI-based light satisfaction evaluation in Plant.update() and per-species light
  hour requirements (lightNeedHours).

---

## Software & Technology

**Java & JavaFX**

- Oracle Corporation. (2023). *Java SE 18 Documentation*.
  https://docs.oracle.com/en/java/index.html
  Used for: Java 18 language features, Collections framework (synchronized lists, ConcurrentLinkedQueue).

- OpenJFX. (2022). *JavaFX 18 Documentation*.
  https://openjfx.io/javadoc/18/
  Used for: AnimationTimer, FXML, Canvas rendering, JavaFX properties and data binding.

- Sharan, K. (2015). *Learn JavaFX 8: Building User Experience and Interfaces with Java 8*.
  Apress.
  Used for: FXML architecture, SplitPane layout, TabPane, and Canvas rendering patterns in
  GardenController and GardenDashboard.fxml.

**Design Patterns**

- Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of
  Reusable Object-Oriented Software*. Addison-Wesley.
  Used for: Singleton pattern (GardenLogger), Observer pattern (SimulationEngine tick
  callbacks), and Strategy pattern (PestControlMethod enum — ORGANIC/CHEMICAL/TARGETED).

- Bloch, J. (2018). *Effective Java* (3rd ed.). Addison-Wesley.
  Used for: Thread-safe collection design, synchronized list usage, and defensive copying
  in GardenLogger.

**Object-Oriented Analysis & Design**

- Larman, C. (2004). *Applying UML and Patterns: An Introduction to Object-Oriented Analysis
  and Design and Iterative Development* (3rd ed.). Prentice Hall.
  Used for: UML class diagram conventions, use case format (user stories + scenarios), and
  OO analysis methodology followed throughout the project.

---

## Course Materials

- CSEN 275 Course Slides. Santa Clara University.
  Used for: Project requirements, UML diagram conventions, and use case document format.

- Java Textbook (as referenced in the course syllabus).
  Used for: JavaFX GUI chapters (last 4 chapters) for implementing the dashboard,
  Canvas rendering, and FXML layout.
