package org.example;

public class Move {
    final boolean wall;

    final direction dir;
    final boolean jump;
    final direction jumpDir;
    final Square start, end, wallBottomLeft;
    final boolean horizontal;

    public Move(Builder builder) {
        this.wall = builder.wall;
        this.dir = builder.dir;
        this.jump = builder.jump;
        this.jumpDir = builder.jumpDir;
        this.start = builder.start;
        this.end = builder.end;
        this.wallBottomLeft = builder.wallBottomLeft;
        this.horizontal = builder.horizontal;

    }


    enum direction {
        N, E, S, W;
    }

    // Static class Builder
    public static class Builder {

        /// instance fields
        private boolean wall;

        private direction dir;
        private boolean jump;
        private direction jumpDir;
        private Square start, end, wallBottomLeft;
        private boolean horizontal;

        // Setter methods
        public Builder setWall(boolean wall) {
            this.wall = wall;
            return this;
        }

        public Builder setDir(direction dir) {
            this.dir = dir;
            return this;
        }

        public Builder setJump(boolean jump) {
            this.jump = jump;
            return this;
        }

        public Builder setJumpDir(direction jumpDir) {
            this.jumpDir = jumpDir;
            return this;
        }

        public Builder setStart(Square start) {
            this.start = start;
            return this;
        }

        public Builder setEnd(Square end) {
            this.end = end;
            return this;
        }

        public Builder setWallBottomLeft(Square wallBottomLeft) {
            this.wallBottomLeft = wallBottomLeft;
            return this;
        }

        public Builder setHorizontal(boolean horizontal) {
            this.horizontal = horizontal;
            return this;
        }

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
        }

        // build method to deal with outer class
        // to return outer instance
        public Move build() {
            return new Move(this);
        }
    }


    @Override
    public String toString(){
        return wall ? (horizontal ? (wallBottomLeft.toString() + "h") : (wallBottomLeft.toString() + "v")) : end.toString();
    }
}
