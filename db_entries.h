#ifndef DB_ENTRIES_H
#include <inttypes.h>

typedef enum {
    ADD,
    DELETE,
    UPDATE
} graph_op;

typedef struct{
    graph_op type;
    uint32_t nodeid;
} graph_op_entry_t;

typedef struct {
    uint32_t streamid;
    uint64_t timestamp;
    double value;
} data_entry_t;
#endif
