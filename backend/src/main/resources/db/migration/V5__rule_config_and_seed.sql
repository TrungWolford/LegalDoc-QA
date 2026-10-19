-- V5: Rule configuration table and seed data

CREATE TABLE rule_config (
    rule_id            VARCHAR(60) PRIMARY KEY,
    name               VARCHAR(150) NOT NULL,
    description        TEXT,
    category           VARCHAR(20) NOT NULL
                       CHECK (category IN ('REFERENCE', 'DEFINITION', 'VALUE', 'STRUCTURE', 'TEXT_QUALITY', 'ENTITY', 'META', 'AI')),
    enabled            BOOLEAN     NOT NULL DEFAULT TRUE,
    default_severity   VARCHAR(10) NOT NULL CHECK (default_severity IN ('ERROR', 'WARNING', 'INFO')),
    severity_override  VARCHAR(10) CHECK (severity_override IN ('ERROR', 'WARNING', 'INFO')),
    params             JSONB       NOT NULL DEFAULT '{}'::jsonb,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed all rules from the detection catalog
INSERT INTO rule_config (rule_id, name, category, default_severity, enabled, params) VALUES
    -- P0 Reference
    ('REF_BROKEN',              'Broken cross-reference',                         'REFERENCE',    'ERROR',   TRUE,  '{}'),
    -- P1 Reference
    ('REF_SELF',                'Self-referencing clause',                         'REFERENCE',    'WARNING', TRUE,  '{}'),
    -- P2 Reference
    ('REF_TARGET_MISMATCH',     'Reference target subject mismatch',              'REFERENCE',    'WARNING', FALSE, '{"minKeywordOverlap": 0.3, "confidenceThreshold": 0.6}'),
    ('REF_CIRCULAR',            'Circular reference chain',                        'REFERENCE',    'WARNING', FALSE, '{}'),
    ('REF_RELATIVE_AMBIGUOUS',  'Ambiguous relative reference',                    'REFERENCE',    'WARNING', FALSE, '{}'),

    -- P0 Definition
    ('TERM_UNDEFINED',          'Undefined capitalized term',                      'DEFINITION',   'WARNING', TRUE,  '{"minOccurrences": 2}'),
    ('TERM_UNUSED',             'Defined but unused term',                         'DEFINITION',   'INFO',    TRUE,  '{}'),
    -- P1 Definition
    ('TERM_DUPLICATE',          'Duplicate definition with different text',         'DEFINITION',   'ERROR',   TRUE,  '{}'),
    ('TERM_VARIANT',            'Inconsistent term variants',                       'DEFINITION',   'WARNING', TRUE,  '{"similarityThreshold": 0.85}'),
    -- P2 Definition
    ('TERM_CIRCULAR_DEF',       'Circular term definition chain',                   'DEFINITION',   'WARNING', FALSE, '{}'),

    -- P0 Value
    ('DATE_LOGIC',              'Contradictory or impossible date ordering',        'VALUE',        'ERROR',   TRUE,  '{"datePairs": []}'),
    ('WORDS_VS_DIGITS',         'Number words do not match digits',                 'VALUE',        'ERROR',   TRUE,  '{}'),
    ('VALUE_CONFLICT',          'Same subject with conflicting values',             'VALUE',        'ERROR',   TRUE,  '{"tolerance": 0.001}'),
    -- P1 Value
    ('PERCENT_SUM',             'Percentage allocation does not sum to expected',   'VALUE',        'ERROR',   TRUE,  '{"expectedTotal": 100}'),
    ('CURRENCY_INCONSISTENT',   'Mixed currencies for same subject',                'VALUE',        'WARNING', TRUE,  '{}'),
    ('RANGE_INVALID',           'Invalid numeric range (min > max)',                 'VALUE',        'ERROR',   TRUE,  '{}'),
    -- P2 Value
    ('DATE_FORMAT_INCONSISTENT','Mixed date formats in document',                   'VALUE',        'INFO',    FALSE, '{}'),

    -- P0 Structure
    ('NUMBERING_DUPLICATE',     'Duplicate section label',                          'STRUCTURE',    'ERROR',   TRUE,  '{}'),
    -- P1 Structure
    ('NUMBERING_GAP',           'Gap in numbering sequence',                        'STRUCTURE',    'WARNING', TRUE,  '{}'),
    ('NUMBERING_ORDER',         'Out-of-order section labels',                      'STRUCTURE',    'WARNING', TRUE,  '{}'),
    ('SUBCLAUSE_ORPHAN',        'Orphaned sub-clause',                              'STRUCTURE',    'WARNING', TRUE,  '{"minSubclauses": 2}'),
    ('CLAUSE_EMPTY',            'Empty clause (heading with no body)',               'STRUCTURE',    'WARNING', TRUE,  '{}'),
    -- P2 Structure
    ('TOC_MISMATCH',            'Table of contents does not match headings',         'STRUCTURE',    'WARNING', FALSE, '{}'),

    -- P0 Text quality
    ('PLACEHOLDER_LEFT',        'Placeholder text left in document',                'TEXT_QUALITY',  'ERROR',   TRUE,  '{}'),
    -- P1 Text quality
    ('TEXT_REPEATED_WORD',       'Repeated word or punctuation',                     'TEXT_QUALITY',  'WARNING', TRUE,  '{}'),
    ('TEXT_UNBALANCED',          'Unbalanced brackets or quotation marks',           'TEXT_QUALITY',  'WARNING', TRUE,  '{}'),
    -- P2 Text quality
    ('TEXT_TYPO',                'Probable typo with suggestion',                    'TEXT_QUALITY',  'INFO',    FALSE, '{"confidenceFloor": 0.7}'),

    -- P1 Entity
    ('PARTY_NAME_INCONSISTENT', 'Inconsistent party name variants',                 'ENTITY',       'WARNING', TRUE,  '{}'),

    -- P2 Entity
    ('DUPLICATE_CLAUSE_TEXT',    'Near-duplicate clause text',                       'ENTITY',       'INFO',    FALSE, '{"jaccardThreshold": 0.85}'),

    -- P0 Meta
    ('PARSE_COVERAGE_LOW',      'Parse coverage below threshold',                   'META',         'WARNING', TRUE,  '{"minCoveragePercent": 70}'),

    -- P2 AI
    ('AI_AMBIGUITY',            'AI-detected ambiguous clause',                     'AI',           'INFO',    FALSE, '{}');
