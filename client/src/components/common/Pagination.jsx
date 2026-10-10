import { Button } from '../ui/button';

// Previous / Next buttons, hidden when there is only one page
const Pagination = ({ page, totalPages, onPageChange }) => {
  if (!totalPages || totalPages <= 1) return null;

  return (
    <div className="mt-6 flex justify-center gap-2">
      <Button variant="outline" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
        Previous
      </Button>
      <span className="flex items-center px-4 text-sm text-muted-foreground">
        Page {page + 1} of {totalPages}
      </span>
      <Button variant="outline" onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}>
        Next
      </Button>
    </div>
  );
};

export default Pagination;
