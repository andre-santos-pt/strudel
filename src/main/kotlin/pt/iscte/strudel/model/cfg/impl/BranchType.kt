package pt.iscte.strudel.model.cfg.impl

/**
 *
 * @author franciscoalfredo
 * @enum BRANCH_TYPE_STATE
 * @description The type of branch that the visitor is currently in.
 */
enum class BranchType {
    ROOT, SELECTION, ALTERNATIVE, LOOP
}