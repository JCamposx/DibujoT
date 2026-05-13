# Gallery Specification

## Purpose

Gallery screen displaying 8 drawable thumbnails in a grid. Allows users to select a drawing and navigate to the send screen.

## Requirements

### Requirement: Display Image Grid

GalleryActivity MUST display all 8 DrawingItems in a 2-column RecyclerView grid using GridLayoutManager.

#### Scenario: Grid renders on launch

- GIVEN the app is launched
- WHEN GalleryActivity is created
- THEN a 2-column grid of 8 thumbnail images is shown
- AND each cell displays the DrawingItem thumbnail

#### Scenario: Empty repository — no items

- GIVEN DrawingRepository returns an empty list
- WHEN GalleryActivity is created
- THEN the RecyclerView renders with zero items and no crash occurs

---

### Requirement: Navigate on Tap

Tapping a grid item MUST navigate to SendActivity, passing the selected DrawingItem as an Intent extra.

#### Scenario: Tap navigates to SendActivity

- GIVEN the gallery is showing 8 items
- WHEN the user taps item at index N
- THEN SendActivity is started
- AND the Intent contains the DrawingItem for index N

#### Scenario: Rapid double-tap does not double-launch

- GIVEN the gallery is visible
- WHEN the user taps the same item twice in quick succession
- THEN only one SendActivity instance is launched
