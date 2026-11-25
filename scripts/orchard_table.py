"""The orchard trees (Trees.ORCHARD in Java, docs/flora-and-food-plan.md "Orchard"): whole wood types like the flora phase 3
fruit trees. Imported by every wood generator so the list lives in one place:  WOODS = [...] + ORCHARD.
tree -> (fruit item, English fruit name). Keep in step with Trees.FRUIT_KINDS, Mechanical.WOODS and Mechanical.LITTER_LEAVES."""
ORCHARD_FRUIT = {
    "pear": ("pears", "Pears"),
    "sweet_cherry": ("sweet_cherries", "Sweet Cherries"),
    "lemon": ("lemons", "Lemons"),
    "clementine": ("clementines", "Clementines"),
    "walnut": ("walnuts", "Walnuts"),
    "chestnut": ("chestnuts", "Chestnuts"),
    "quince": ("quinces", "Quinces"),
    "mulberry": ("mulberries", "Mulberries"),
    "medlar": ("medlars", "Medlars"),
    "peach": ("peaches", "Peaches"),
    "apricot": ("apricots", "Apricots"),
    "almond": ("almonds", "Almonds"),
    "pomegranate": ("pomegranates", "Pomegranates"),
}
ORCHARD = list(ORCHARD_FRUIT)
EVERGREEN = ["lemon", "clementine"]  # no bare winter look (SeasonalFoliage.DECIDUOUS leaves them out)
